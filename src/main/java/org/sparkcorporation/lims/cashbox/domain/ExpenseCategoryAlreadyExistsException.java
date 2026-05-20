package org.sparkcorporation.lims.cashbox.domain;

/**
 * Raised when an {@code ExpenseCategory.create} call collides with an existing row on {@code code}.
 * Mapped to {@code 409 Conflict} by {@code ProblemDetailAdvice}.
 */
public class ExpenseCategoryAlreadyExistsException extends RuntimeException {

    public ExpenseCategoryAlreadyExistsException(String code) {
        super("ExpenseCategory with code already exists: " + code);
    }
}
