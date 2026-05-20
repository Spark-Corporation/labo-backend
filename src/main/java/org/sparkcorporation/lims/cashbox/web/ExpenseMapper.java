package org.sparkcorporation.lims.cashbox.web;

import org.sparkcorporation.lims.cashbox.domain.Expense;

final class ExpenseMapper {
    private ExpenseMapper() {}

    static ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getCategoryId(),
                e.getMode(),
                e.getAmount(),
                e.getDescription(),
                e.getJustificationScanUri(),
                e.getCashRegisterId(),
                e.getRecordedBy(),
                e.getRecordedAt());
    }
}
