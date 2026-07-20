package org.sparkcorporation.lims.billing.domain;

import org.sparkcorporation.lims.iam.api.UserId;

/**
 * Raised by {@code CashRegisterApi.assignUsers} when at least one of the supplied user ULIDs does
 * not resolve via {@link org.sparkcorporation.lims.iam.api.UserApi#existsById}. Mapped to {@code
 * 404 Not Found} via {@code BillingProblemDetailAdvice}; the whole call is rejected (no partial
 * assignment).
 */
public class CashRegisterInvalidUserReferenceException extends RuntimeException {

    private final String userId;

    public CashRegisterInvalidUserReferenceException(UserId userId) {
        super("user not found: " + userId.value());
        this.userId = userId.value();
    }

    public String userId() {
        return userId;
    }
}
