package org.sparkcorporation.lims.billing.web;

import java.time.Clock;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.InvoiceApi;
import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.PaymentId;
import org.sparkcorporation.lims.billing.api.PaymentRecorded;
import org.sparkcorporation.lims.billing.domain.CashRegisterNotAssignedException;
import org.sparkcorporation.lims.billing.domain.NewPayment;
import org.sparkcorporation.lims.billing.domain.Payment;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidCashRegisterReferenceException;
import org.sparkcorporation.lims.billing.domain.PaymentInvalidLabFileReferenceException;
import org.sparkcorporation.lims.billing.infra.PaymentRepository;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Domain service backing {@link PaymentController} (spec §7.4).
 *
 * <p>The {@code record} method enforces three guards:
 *
 * <ol>
 *   <li>the {@code labFileId} resolves via {@link LabFileApi#existsById} (else 404);
 *   <li>the {@code cashRegisterId} resolves via {@link CashRegisterApi#existsById} (else 404);
 *   <li>the calling user is in the assigned-user set of that register (else 403).
 * </ol>
 *
 * <p>If an invoice already exists for the lab-file at insert time, the payment is linked
 * immediately. Otherwise the link is left null — the back-fill query in {@code
 * BillingFinalizationService} will populate it once bio-validation finalizes the invoice.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final LabFileApi labFileApi;
    private final CashRegisterApi cashRegisterApi;
    private final InvoiceApi invoiceApi;
    private final PaymentMapper mapper;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    @Transactional
    public PaymentResponse record(PaymentRequest req, String callingUserId) {
        if (!labFileApi.existsById(new LabFileId(req.labFileId()))) {
            throw new PaymentInvalidLabFileReferenceException(req.labFileId());
        }
        var registerId = new CashRegisterId(req.cashRegisterId());
        if (!cashRegisterApi.existsById(registerId)) {
            throw new PaymentInvalidCashRegisterReferenceException(req.cashRegisterId());
        }
        if (!cashRegisterApi.isUserAssigned(registerId, new UserId(callingUserId))) {
            throw new CashRegisterNotAssignedException(req.cashRegisterId(), callingUserId);
        }

        var payment =
                Payment.record(
                        new NewPayment(
                                req.labFileId(),
                                req.mode(),
                                req.amount(),
                                req.cashRegisterId(),
                                callingUserId,
                                req.note()),
                        clock);

        // If an invoice already exists for this lab-file, link immediately.
        invoiceApi
                .findByLabFileId(new LabFileId(req.labFileId()))
                .ifPresent(inv -> payment.linkToInvoice(new InvoiceId(inv.id().value())));

        var saved = paymentRepo.save(payment);
        publisher.publishEvent(
                new PaymentRecorded(
                        new PaymentId(saved.getId()),
                        new LabFileId(saved.getLabFileId()),
                        saved.getMode(),
                        saved.getAmount(),
                        saved.getRecordedAt()));
        return mapper.toResponse(saved);
    }

    public List<PaymentResponse> findByLabFileId(LabFileId labFileId) {
        return paymentRepo.findByLabFileIdOrderByRecordedAtAsc(labFileId.value()).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
