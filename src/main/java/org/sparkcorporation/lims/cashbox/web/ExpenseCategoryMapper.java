package org.sparkcorporation.lims.cashbox.web;

import org.sparkcorporation.lims.cashbox.domain.ExpenseCategory;

final class ExpenseCategoryMapper {
    private ExpenseCategoryMapper() {}

    static ExpenseCategoryResponse toResponse(ExpenseCategory c) {
        return new ExpenseCategoryResponse(
                c.getId(),
                c.getCode(),
                c.getLabel(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
