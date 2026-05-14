package org.sparkcorporation.lims.billing.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.billing.api.CashRegisterId;

/**
 * Raised when a cash-register lookup misses. Extends {@link EntityNotFoundException} so the shared
 * {@code ProblemDetailAdvice} maps it to {@code 404 Not Found}.
 */
public class CashRegisterNotFoundException extends EntityNotFoundException {

    public CashRegisterNotFoundException(CashRegisterId id) {
        super("cash register not found: " + id.value());
    }
}
