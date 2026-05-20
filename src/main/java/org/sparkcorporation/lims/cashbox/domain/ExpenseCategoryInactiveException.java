package org.sparkcorporation.lims.cashbox.domain;

import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;

public class ExpenseCategoryInactiveException extends RuntimeException {

    public ExpenseCategoryInactiveException(ExpenseCategoryId id) {
        super("Expense category is inactive: " + id.value());
    }
}
