package org.sparkcorporation.lims.cashbox.web;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.cashbox.domain.Expense;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryInactiveException;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryNotFoundException;
import org.sparkcorporation.lims.cashbox.domain.JournalEntry;
import org.sparkcorporation.lims.cashbox.infra.ExpenseCategoryRepository;
import org.sparkcorporation.lims.cashbox.infra.ExpenseRepository;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRepository;
import org.sparkcorporation.lims.iam.api.UserId;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepo;
    private final JournalEntryRepository journalRepo;
    private final ExpenseCategoryRepository categoryRepo;
    private final CashRegisterApi cashRegisterApi;
    private final Clock clock;

    @Transactional
    public ExpenseResponse record(ExpenseRequest req, UserId by) {
        var categoryId = new ExpenseCategoryId(req.categoryId());
        var cashRegisterId = new CashRegisterId(req.cashRegisterId());
        var category =
                categoryRepo
                        .findById(categoryId.value())
                        .orElseThrow(() -> new ExpenseCategoryNotFoundException(categoryId));
        if (!category.isActive()) {
            throw new ExpenseCategoryInactiveException(categoryId);
        }
        if (!cashRegisterApi.existsById(cashRegisterId)) {
            throw new IllegalArgumentException(
                    "cashRegisterId does not exist: " + cashRegisterId.value());
        }
        var expense =
                Expense.record(
                        categoryId,
                        req.mode(),
                        req.amount(),
                        req.description(),
                        req.justificationScanUri(),
                        cashRegisterId,
                        by,
                        clock);
        var saved = expenseRepo.save(expense);
        var journal =
                JournalEntry.forExpense(
                        saved.getId(),
                        cashRegisterId,
                        by,
                        saved.getMode(),
                        saved.getAmount(),
                        saved.getRecordedAt());
        journalRepo.insertIdempotent(
                journal.getId(),
                journal.getOperationKind().name(),
                journal.getSourceId(),
                journal.getCashRegisterId(),
                journal.getUserId(),
                journal.getLabFileId(),
                journal.getPatientId(),
                journal.getPaymentMode() == null ? null : journal.getPaymentMode().name(),
                journal.getAmount(),
                journal.getOccurredAt());
        return ExpenseMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> list(ExpenseFilters filters, Pageable pageable) {
        return expenseRepo
                .search(
                        filters.from(),
                        filters.to(),
                        filters.categoryId() == null ? null : filters.categoryId().value(),
                        filters.recordedBy() == null ? null : filters.recordedBy().value(),
                        filters.cashRegisterId() == null ? null : filters.cashRegisterId().value(),
                        pageable)
                .map(ExpenseMapper::toResponse);
    }
}
