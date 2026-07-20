package org.sparkcorporation.lims.billing.infra.pdf;

import org.sparkcorporation.lims.billing.api.PaymentId;

/**
 * Thrown by {@link PaymentReceiptModelAssembler#assemble(PaymentId)} when no row matches the id.
 * Mapped to HTTP 404 by {@code billing/web/BillingProblemDetailAdvice} (extended in chunk 2).
 */
public final class PaymentInvalidIdException extends RuntimeException {

    private final PaymentId paymentId;

    public PaymentInvalidIdException(PaymentId paymentId) {
        super("payment not found: " + paymentId.value());
        this.paymentId = paymentId;
    }

    public PaymentId paymentId() {
        return paymentId;
    }
}
