package org.sparkcorporation.lims.cashbox.domain;

import jakarta.persistence.EntityNotFoundException;

/**
 * Raised when an expense lookup misses. Extends {@link EntityNotFoundException} so the shared
 * {@code ProblemDetailAdvice} maps it to {@code 404 Not Found}.
 */
public class ExpenseNotFoundException extends EntityNotFoundException {

    public ExpenseNotFoundException(String expenseId) {
        super("expense not found: " + expenseId);
    }
}
