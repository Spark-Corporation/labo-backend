package org.sparkcorporation.lims.cashbox.domain;

import jakarta.persistence.EntityNotFoundException;

/**
 * Raised when a home-collection-fee lookup misses. Extends {@link EntityNotFoundException} so the
 * shared {@code ProblemDetailAdvice} maps it to {@code 404 Not Found}.
 */
public class HomeCollectionFeeNotFoundException extends EntityNotFoundException {

    public HomeCollectionFeeNotFoundException(String feeId) {
        super("home collection fee not found: " + feeId);
    }
}
