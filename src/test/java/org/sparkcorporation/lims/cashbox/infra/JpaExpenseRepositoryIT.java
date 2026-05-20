package org.sparkcorporation.lims.cashbox.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.domain.Expense;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategory;
import org.sparkcorporation.lims.iam.api.UserId;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaExpenseRepositoryIT extends IntegrationTestBase {

    @Autowired private ExpenseRepository repo;
    @Autowired private ExpenseCategoryRepository categoryRepo;

    private static final UserId BY = new UserId("01KQZ0WPVQ9EKB5AZXKWS0AYYJ");

    @Test
    void persistAndSearchRoundTrip() {
        var clock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
        var register = CashRegisterId.generate();
        var category = categoryRepo.save(ExpenseCategory.create("RIT-A", "RIT A", BY, clock));

        var ex =
                Expense.record(
                        category.expenseCategoryId(),
                        PaymentMode.CASH,
                        new BigDecimal("12500.00"),
                        "Water",
                        null,
                        register,
                        BY,
                        clock);
        repo.save(ex);

        var page = repo.search(null, null, null, null, null, PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(Expense::getId).contains(ex.getId());
    }

    @Test
    void searchFiltersByDateRange() {
        var early = Clock.fixed(Instant.parse("2026-05-17T10:00:00Z"), ZoneOffset.UTC);
        var late = Clock.fixed(Instant.parse("2026-05-19T10:00:00Z"), ZoneOffset.UTC);
        var register = CashRegisterId.generate();
        var cat = categoryRepo.save(ExpenseCategory.create("RIT-B", "RIT B", BY, early));

        var earlyEx =
                Expense.record(
                        cat.expenseCategoryId(),
                        PaymentMode.CASH,
                        new BigDecimal("100"),
                        "early",
                        null,
                        register,
                        BY,
                        early);
        var lateEx =
                Expense.record(
                        cat.expenseCategoryId(),
                        PaymentMode.CASH,
                        new BigDecimal("200"),
                        "late",
                        null,
                        register,
                        BY,
                        late);
        repo.save(earlyEx);
        repo.save(lateEx);

        var fromMid = Instant.parse("2026-05-18T00:00:00Z");
        var fromMidPage = repo.search(fromMid, null, null, null, null, PageRequest.of(0, 10));
        assertThat(fromMidPage.getContent())
                .extracting(Expense::getId)
                .contains(lateEx.getId())
                .doesNotContain(earlyEx.getId());
    }

    @Test
    void searchFiltersByCategory() {
        var clock = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
        var register = CashRegisterId.generate();
        var cat1 = categoryRepo.save(ExpenseCategory.create("RIT-C", "RIT C", BY, clock));
        var cat2 = categoryRepo.save(ExpenseCategory.create("RIT-D", "RIT D", BY, clock));

        var ex1 =
                Expense.record(
                        cat1.expenseCategoryId(),
                        PaymentMode.CASH,
                        new BigDecimal("100"),
                        "e1",
                        null,
                        register,
                        BY,
                        clock);
        var ex2 =
                Expense.record(
                        cat2.expenseCategoryId(),
                        PaymentMode.CASH,
                        new BigDecimal("200"),
                        "e2",
                        null,
                        register,
                        BY,
                        clock);
        repo.save(ex1);
        repo.save(ex2);

        var byCat1 =
                repo.search(
                        null,
                        null,
                        cat1.expenseCategoryId().value(),
                        null,
                        null,
                        PageRequest.of(0, 10));
        assertThat(byCat1.getContent())
                .extracting(Expense::getId)
                .contains(ex1.getId())
                .doesNotContain(ex2.getId());
    }
}
