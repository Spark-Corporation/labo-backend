package org.sparkcorporation.lims.cashbox.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;

/**
 * Raised when an expense-category lookup misses. Extends {@link EntityNotFoundException} so the
 * shared {@code ProblemDetailAdvice} maps it to {@code 404 Not Found}.
 */
public class ExpenseCategoryNotFoundException extends EntityNotFoundException {

    public ExpenseCategoryNotFoundException(ExpenseCategoryId id) {
        super("ExpenseCategory not found: " + id.value());
    }
}
