package org.sparkcorporation.lims.billing.infra;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.InvoiceApi;
import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.InvoiceNumber;
import org.sparkcorporation.lims.billing.api.InvoiceSummary;
import org.sparkcorporation.lims.billing.domain.Invoice;
import org.sparkcorporation.lims.billing.domain.InvoiceLine;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Read-only {@link InvoiceApi} implementation. Write paths (manual issuance) live in {@code
 * BillingFinalizationService} — this implementation never invokes them.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultInvoiceApi implements InvoiceApi {

    private final InvoiceRepository repo;
    private final LabFileApi labFileApi;

    @Override
    public Optional<InvoiceSummary> findById(InvoiceId id) {
        return repo.findById(id.value()).map(DefaultInvoiceApi::toSummary);
    }

    @Override
    public Optional<InvoiceSummary> findByLabFileId(LabFileId labFileId) {
        return repo.findByLabFileId(labFileId.value()).map(DefaultInvoiceApi::toSummary);
    }

    @Override
    public Optional<InvoiceSummary> findByNumber(InvoiceNumber number) {
        return repo.findByNumber(number.value()).map(DefaultInvoiceApi::toSummary);
    }

    @Override
    public Map<LabFileId, InvoiceSummary> findAllByLabFileIds(Set<LabFileId> labFileIds) {
        if (labFileIds.isEmpty()) {
            return Map.of();
        }
        var rawIds =
                labFileIds.stream().map(LabFileId::value).collect(Collectors.toUnmodifiableSet());
        return repo.findAllByLabFileIdIn(rawIds).stream()
                .map(DefaultInvoiceApi::toSummary)
                .collect(Collectors.toUnmodifiableMap(InvoiceSummary::labFileId, s -> s));
    }

    @Override
    public Page<InvoiceSummary> search(
            @Nullable Instant from,
            @Nullable Instant to,
            @Nullable InvoiceCustomerKind customerKind,
            @Nullable String customerId,
            @Nullable LabFileId labFileId,
            @Nullable String patientId,
            Pageable pageable) {

        @Nullable List<String> labFileIds;
        if (patientId != null) {
            // Resolve the patient's lab files via the cross-module API. Cap-the-list semantics:
            // patients with >1000 lab files would overflow the SQL IN-list anyway. For 1.6.a this
            // is acceptable; a join-based projection is a 1.6.b candidate if it becomes a problem.
            var resolved =
                    labFileApi.findActiveByPatient(patientId).stream()
                            .map(s -> s.id().value())
                            .toList();
            if (labFileId != null && !resolved.contains(labFileId.value())) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            labFileIds = labFileId != null ? List.of(labFileId.value()) : resolved;
            if (labFileIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
        } else if (labFileId != null) {
            labFileIds = List.of(labFileId.value());
        } else {
            labFileIds = null;
        }

        return repo.search(from, to, customerKind, customerId, labFileIds, pageable)
                .map(DefaultInvoiceApi::toSummary);
    }

    static InvoiceSummary toSummary(Invoice i) {
        var lineSummaries = i.linesView().stream().map(DefaultInvoiceApi::toLineSummary).toList();
        return new InvoiceSummary(
                InvoiceId.of(i.getId()),
                new InvoiceNumber(i.getNumber()),
                new LabFileId(i.getLabFileId()),
                i.getIssuedAt(),
                i.getCustomerKind(),
                i.getCustomerId(),
                i.getCustomerNameSnapshot(),
                i.getHomeCollectionFeeSnapshot(),
                i.getAdditionalFeesSnapshot(),
                i.getDiscountKindSnapshot(),
                i.getDiscountValueSnapshot(),
                i.getTotalAmount(),
                i.getInsuranceCoverageAmount(),
                i.getDiscountAmount(),
                i.getNetAmount(),
                lineSummaries);
    }

    private static InvoiceSummary.LineSummary toLineSummary(InvoiceLine l) {
        return new InvoiceSummary.LineSummary(
                l.getId(),
                l.getPosition(),
                AnalysisId.of(l.getAnalysisId()),
                l.getAnalysisCodeSnapshot(),
                l.getAnalysisNameSnapshot(),
                l.getIndiciaryValueBSnapshot(),
                l.getBRateSnapshot(),
                l.getLabelCountSnapshot(),
                l.getLineTotal());
    }
}
