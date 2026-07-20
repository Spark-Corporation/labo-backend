package org.sparkcorporation.lims.billing.infra;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.InvoiceIssued;
import org.sparkcorporation.lims.billing.domain.BillingSnapshot;
import org.sparkcorporation.lims.billing.domain.Customer;
import org.sparkcorporation.lims.billing.domain.Customer.InsuranceCustomer;
import org.sparkcorporation.lims.billing.domain.Customer.PartnerCustomer;
import org.sparkcorporation.lims.billing.domain.Customer.PatientCustomer;
import org.sparkcorporation.lims.billing.domain.Invoice;
import org.sparkcorporation.lims.billing.domain.PricingEngine;
import org.sparkcorporation.lims.billing.domain.PricingEngine.PricingContext;
import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.shared.money.PricingProperties;

/**
 * Single entry point for invoice finalization — shared by the auto path ({@code
 * BillingFinalizationListener}) and the manual override path ({@code POST /api/v1/invoices}).
 *
 * <p>Per spec §8.1: the listener fires on each {@link
 * org.sparkcorporation.lims.result.api.BiologicalValidationDone} per-AnalysisRequest. Two safety
 * nets guarantee idempotency on a labFile:
 *
 * <ol>
 *   <li>Pre-write idempotency check ({@link InvoiceRepository#findByLabFileId}) — same event
 *       arriving twice (Spring Modulith at-least-once) returns the existing invoice.
 *   <li>"All bio-validated" gate — if any analysis on the lab-file is not yet biologically
 *       validated, the call returns {@code Optional.empty()} (auto path) or the manual-issue caller
 *       surfaces a 422 via the caller's own guard.
 * </ol>
 *
 * <p>The 1:1 UNIQUE constraint on {@code invoices.lab_file_id} is the database backstop in case
 * both checks race past each other.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingFinalizationService {

    private final InvoiceRepository invoiceRepo;
    private final PaymentRepository paymentRepo;
    private final LabFileApi labFileApi;
    private final AnalysisApi analysisApi;
    private final InsuranceApi insuranceApi;
    private final PartnerApi partnerApi;
    private final ResultApi resultApi;
    private final PricingProperties pricingProperties;
    private final InvoiceNumberGenerator numberGenerator;
    private final PricingEngine pricingEngine;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    /**
     * Finalize the invoice for {@code labFileId}. Returns the persisted (or pre-existing) invoice
     * when ready; {@link Optional#empty()} when the lab-file is not yet fully bio-validated. The
     * caller decides what to do with empty — auto listener no-ops, manual controller may map to
     * 422.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<Invoice> finalize(LabFileId labFileId) {
        var existing = invoiceRepo.findByLabFileId(labFileId.value());
        if (existing.isPresent()) {
            log.debug("invoice already exists for labFileId={}", labFileId.value());
            return existing;
        }

        LabFileSummary labFile = requireLabFile(labFileId);
        if (!allAnalysesBiologicallyValidated(labFile)) {
            log.debug(
                    "labFileId={} not all bio-validated yet; deferring finalization",
                    labFileId.value());
            return Optional.empty();
        }
        return Optional.of(issueInvoiceFor(labFile, labFileId));
    }

    private LabFileSummary requireLabFile(LabFileId labFileId) {
        return labFileApi
                .findById(labFileId)
                .orElseThrow(
                        () ->
                                new org.sparkcorporation.lims.billing.domain
                                        .InvoiceNotFoundException(
                                        "lab file not found: " + labFileId.value()));
    }

    private Invoice issueInvoiceFor(LabFileSummary labFile, LabFileId labFileId) {
        var requests = labFile.analyses();
        var analysisIds = requests.stream().map(r -> AnalysisId.of(r.analysisId())).toList();
        var analyses = analysisApi.findAllByIds(analysisIds);

        var ctxAndCustomer = resolveContextAndCustomer(labFile);
        var lineInputs = buildLineInputs(requests, analyses, labFileId);
        var billingBlock = labFile.billing();
        var pricing =
                pricingEngine.price(
                        lineInputs,
                        billingBlock.homeCollectionFee(),
                        billingBlock.additionalFees(),
                        billingBlock.discountKind(),
                        billingBlock.discountValue(),
                        ctxAndCustomer.context());

        var today = LocalDate.ofInstant(clock.instant(), ZoneId.systemDefault());
        var number = numberGenerator.next(today);
        var snap =
                new BillingSnapshot(
                        billingBlock.homeCollectionFee(),
                        billingBlock.additionalFees(),
                        billingBlock.discountKind(),
                        billingBlock.discountValue());
        var issuedAt = clock.instant();
        var invoice =
                Invoice.issue(
                        number, labFileId, ctxAndCustomer.customer(), snap, pricing, issuedAt);
        invoice.populateLineSnapshots(requests, analyses, pricing);

        var saved = invoiceRepo.save(invoice);
        backFillPayments(labFileId, saved);
        publisher.publishEvent(
                new InvoiceIssued(
                        saved.invoiceId(),
                        saved.invoiceNumber(),
                        labFileId,
                        saved.getNetAmount(),
                        issuedAt));
        return saved;
    }

    private List<PricingEngine.LineInput> buildLineInputs(
            List<LabFileSummary.AnalysisRequestSummary> requests,
            java.util.Map<
                            AnalysisId,
                            ? extends org.sparkcorporation.lims.catalog.api.AnalysisSummary>
                    analyses,
            LabFileId labFileId) {
        return requests.stream()
                .map(
                        r -> {
                            var a = analyses.get(AnalysisId.of(r.analysisId()));
                            if (a == null) {
                                throw new IllegalStateException(
                                        "analysis "
                                                + r.analysisId()
                                                + " on lab-file "
                                                + labFileId.value()
                                                + " not in active catalog");
                            }
                            return new PricingEngine.LineInput(a.indiciaryValueB(), a.labelCount());
                        })
                .toList();
    }

    private void backFillPayments(LabFileId labFileId, Invoice saved) {
        int linked =
                paymentRepo.linkPaymentsToInvoice(labFileId.value(), saved.invoiceId().value());
        if (linked > 0) {
            log.debug(
                    "back-filled invoice_id={} on {} payment(s) for labFileId={}",
                    saved.invoiceId().value(),
                    linked,
                    labFileId.value());
        }
    }

    private boolean allAnalysesBiologicallyValidated(LabFileSummary labFile) {
        if (labFile.analyses().isEmpty()) {
            return false;
        }
        var labFileId = labFile.id();
        var results = resultApi.findActiveByLabFile(labFileId);
        if (results.size() < labFile.analyses().size()) {
            return false;
        }
        return results.stream().allMatch(r -> r.state() == ResultState.BIOLOGICAL_VALIDATED);
    }

    private ContextAndCustomer resolveContextAndCustomer(LabFileSummary labFile) {
        var partner = labFile.partner();
        if (partner != null) {
            var resolved =
                    partnerApi
                            .findById(partner.id())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "labFile "
                                                            + labFile.id().value()
                                                            + " references unknown partnerId="
                                                            + partner.id().value()));
            return new ContextAndCustomer(
                    new PricingContext.PartnerRouted(resolved.bRate()),
                    new PartnerCustomer(resolved.id().value(), resolved.name()));
        }
        var insurance = labFile.insurance();
        if (insurance != null) {
            var resolved =
                    insuranceApi
                            .findById(insurance.id())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "labFile "
                                                            + labFile.id().value()
                                                            + " references unknown insuranceId="
                                                            + insurance.id().value()));
            return new ContextAndCustomer(
                    new PricingContext.Insured(resolved.bRate(), resolved.coverageRate()),
                    new InsuranceCustomer(resolved.id().value(), resolved.name()));
        }
        var patient = labFile.patient();
        var name = patient.labCode() + " " + patient.lastName();
        return new ContextAndCustomer(
                new PricingContext.SelfPay(pricingProperties.bRate()),
                new PatientCustomer(patient.id(), name));
    }

    private record ContextAndCustomer(PricingContext context, Customer customer) {}
}
