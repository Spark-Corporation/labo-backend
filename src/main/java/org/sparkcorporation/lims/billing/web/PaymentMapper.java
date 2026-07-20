package org.sparkcorporation.lims.billing.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.billing.domain.Payment;

/** Maps {@link Payment} domain aggregates → {@link PaymentResponse} JSON DTOs. */
@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getLabFileId(),
                p.getInvoiceId(),
                p.getMode(),
                p.getAmount(),
                p.getCashRegisterId(),
                p.getRecordedByUserId(),
                p.getRecordedAt(),
                p.getNote());
    }
}
