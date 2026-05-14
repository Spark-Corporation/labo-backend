package org.sparkcorporation.lims.billing.domain;

/**
 * Raised by {@code PaymentService.record} when the calling user is not in the assigned-user set of
 * the target cash register (CDC §VIII: "only the cashier assigned to a till may collect through
 * it"). Mapped to {@code 403 Forbidden} via {@code BillingProblemDetailAdvice}.
 */
public class CashRegisterNotAssignedException extends RuntimeException {

    private final String cashRegisterId;
    private final String userId;

    public CashRegisterNotAssignedException(String cashRegisterId, String userId) {
        super("user " + userId + " is not assigned to cash register " + cashRegisterId);
        this.cashRegisterId = cashRegisterId;
        this.userId = userId;
    }

    public String cashRegisterId() {
        return cashRegisterId;
    }

    public String userId() {
        return userId;
    }
}
