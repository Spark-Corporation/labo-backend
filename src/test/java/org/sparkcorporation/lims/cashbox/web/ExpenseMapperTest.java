package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.ExpenseCategoryId;
import org.sparkcorporation.lims.cashbox.domain.Expense;
import org.sparkcorporation.lims.iam.api.UserId;

class ExpenseMapperTest {

    @Test
    void mapsAllFields() {
        var clock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
        var by = UserId.next();
        var register = CashRegisterId.generate();
        var category = ExpenseCategoryId.generate();
        var domain =
                Expense.record(
                        category,
                        PaymentMode.CASH,
                        new BigDecimal("12500.00"),
                        "Water",
                        "s3://lims/expenses/abc.pdf",
                        register,
                        by,
                        clock);

        var response = ExpenseMapper.toResponse(domain);

        assertThat(response.id()).isEqualTo(domain.getId());
        assertThat(response.categoryId()).isEqualTo(category.value());
        assertThat(response.mode()).isEqualTo(PaymentMode.CASH);
        assertThat(response.amount()).isEqualByComparingTo("12500.00");
        assertThat(response.description()).isEqualTo("Water");
        assertThat(response.justificationScanUri()).isEqualTo("s3://lims/expenses/abc.pdf");
        assertThat(response.cashRegisterId()).isEqualTo(register.value());
        assertThat(response.recordedBy()).isEqualTo(by.value());
        assertThat(response.recordedAt()).isEqualTo(clock.instant());
    }
}
