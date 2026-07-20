package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.cashbox.domain.Expense;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategory;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryInactiveException;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategoryNotFoundException;
import org.sparkcorporation.lims.cashbox.infra.ExpenseCategoryRepository;
import org.sparkcorporation.lims.cashbox.infra.ExpenseRepository;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRepository;
import org.sparkcorporation.lims.iam.api.UserId;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
    private static final UserId BY = UserId.next();
    private static final CashRegisterId REGISTER = CashRegisterId.generate();

    @Mock private ExpenseRepository expenseRepo;
    @Mock private JournalEntryRepository journalRepo;
    @Mock private ExpenseCategoryRepository categoryRepo;
    @Mock private CashRegisterApi cashRegisterApi;

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        service =
                new ExpenseService(expenseRepo, journalRepo, categoryRepo, cashRegisterApi, FIXED);
    }

    @Test
    void recordingExpenseProducesJournalEntryInSameTx() {
        var activeCategory = ExpenseCategory.create("X", "Y", BY, FIXED);
        var catId = activeCategory.expenseCategoryId();
        when(categoryRepo.findById(catId.value())).thenReturn(Optional.of(activeCategory));
        when(cashRegisterApi.existsById(REGISTER)).thenReturn(true);
        when(expenseRepo.save(any(Expense.class))).thenAnswer(a -> a.getArgument(0));

        var req =
                new ExpenseRequest(
                        catId.value(),
                        PaymentMode.CASH,
                        new BigDecimal("12500"),
                        "Water",
                        null,
                        REGISTER.value());
        var response = service.record(req, BY);

        assertThat(response.amount()).isEqualByComparingTo("12500");
        verify(expenseRepo, times(1)).save(any(Expense.class));
        verify(journalRepo, times(1))
                .insertIdempotent(
                        anyString(),
                        eq("EXPENSE"),
                        anyString(),
                        eq(REGISTER.value()),
                        eq(BY.value()),
                        eq(null),
                        eq(null),
                        eq("CASH"),
                        any(BigDecimal.class),
                        any(Instant.class));
    }

    @Test
    void rejectsUnknownCategory() {
        var catId = ExpenseCategoryId.generate();
        when(categoryRepo.findById(catId.value())).thenReturn(Optional.empty());

        var req =
                new ExpenseRequest(
                        catId.value(),
                        PaymentMode.CASH,
                        new BigDecimal("100"),
                        "x",
                        null,
                        REGISTER.value());
        assertThatThrownBy(() -> service.record(req, BY))
                .isInstanceOf(ExpenseCategoryNotFoundException.class);
    }

    @Test
    void rejectsInactiveCategory() {
        var inactive = ExpenseCategory.create("X", "Y", BY, FIXED);
        inactive.setActive(false, BY, FIXED);
        var catId = inactive.expenseCategoryId();
        when(categoryRepo.findById(catId.value())).thenReturn(Optional.of(inactive));

        var req =
                new ExpenseRequest(
                        catId.value(),
                        PaymentMode.CASH,
                        new BigDecimal("100"),
                        "x",
                        null,
                        REGISTER.value());
        assertThatThrownBy(() -> service.record(req, BY))
                .isInstanceOf(ExpenseCategoryInactiveException.class);
    }

    @Test
    void rejectsUnknownCashRegister() {
        var activeCategory = ExpenseCategory.create("X", "Y", BY, FIXED);
        var catId = activeCategory.expenseCategoryId();
        when(categoryRepo.findById(catId.value())).thenReturn(Optional.of(activeCategory));
        when(cashRegisterApi.existsById(REGISTER)).thenReturn(false);

        var req =
                new ExpenseRequest(
                        catId.value(),
                        PaymentMode.CASH,
                        new BigDecimal("100"),
                        "x",
                        null,
                        REGISTER.value());
        assertThatThrownBy(() -> service.record(req, BY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cashRegisterId");
    }
}
