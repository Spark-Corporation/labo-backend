package org.sparkcorporation.lims.cashbox.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.iam.api.UserId;

class ExpenseTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
    private static final UserId BY = UserId.next();
    private static final CashRegisterId REGISTER = CashRegisterId.generate();
    private static final ExpenseCategoryId CATEGORY = ExpenseCategoryId.generate();

    @Test
    void recordSetsAllFieldsAndAuditTimestamps() {
        var ex =
                Expense.record(
                        CATEGORY,
                        PaymentMode.CASH,
                        new BigDecimal("12500.00"),
                        "Office water bottles",
                        null,
                        REGISTER,
                        BY,
                        FIXED);

        assertThat(ex.getCategoryId()).isEqualTo(CATEGORY.value());
        assertThat(ex.getMode()).isEqualTo(PaymentMode.CASH);
        assertThat(ex.getAmount()).isEqualByComparingTo("12500.00");
        assertThat(ex.getDescription()).isEqualTo("Office water bottles");
        assertThat(ex.getJustificationScanUri()).isNull();
        assertThat(ex.getCashRegisterId()).isEqualTo(REGISTER.value());
        assertThat(ex.getRecordedBy()).isEqualTo(BY.value());
        assertThat(ex.getRecordedAt()).isEqualTo(FIXED.instant());
        assertThat(ex.getCreatedAt()).isEqualTo(FIXED.instant());
        assertThat(ex.getUpdatedAt()).isEqualTo(FIXED.instant());
    }

    @Test
    void recordRejectsNonPositiveAmount() {
        assertThatThrownBy(
                        () ->
                                Expense.record(
                                        CATEGORY,
                                        PaymentMode.CASH,
                                        BigDecimal.ZERO,
                                        "x",
                                        null,
                                        REGISTER,
                                        BY,
                                        FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(
                        () ->
                                Expense.record(
                                        CATEGORY,
                                        PaymentMode.CASH,
                                        new BigDecimal("-5"),
                                        "x",
                                        null,
                                        REGISTER,
                                        BY,
                                        FIXED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordAcceptsOptionalScanUri() {
        var ex =
                Expense.record(
                        CATEGORY,
                        PaymentMode.CASH,
                        new BigDecimal("100"),
                        "x",
                        "s3://lims-attachments/expenses/abc.pdf",
                        REGISTER,
                        BY,
                        FIXED);
        assertThat(ex.getJustificationScanUri())
                .isEqualTo("s3://lims-attachments/expenses/abc.pdf");
    }
}
