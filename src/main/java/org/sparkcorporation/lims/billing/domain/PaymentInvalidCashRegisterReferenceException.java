package org.sparkcorporation.lims.billing.domain;

/**
 * Raised by {@code PaymentService.record} when the supplied {@code cashRegisterId} does not
 * resolve. Mapped to {@code 404 Not Found} via {@code BillingProblemDetailAdvice}.
 */
public class PaymentInvalidCashRegisterReferenceException extends RuntimeException {

    private final String cashRegisterId;

    public PaymentInvalidCashRegisterReferenceException(String cashRegisterId) {
        super("cash register not found: " + cashRegisterId);
        this.cashRegisterId = cashRegisterId;
    }

    public String cashRegisterId() {
        return cashRegisterId;
    }
}
