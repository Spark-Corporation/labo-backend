package org.sparkcorporation.lims.cashbox.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.cashbox.domain.ExpenseCategory;
import org.sparkcorporation.lims.iam.api.UserId;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaExpenseCategoryRepositoryIT extends IntegrationTestBase {

    @Autowired private ExpenseCategoryRepository repo;

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
    private static final UserId BY = new UserId("01KRXY49BFD2BJ9YY1NEFXF8GW");

    @Test
    void persistAndFindByCodeRoundTrip() {
        var cat = ExpenseCategory.create("UTILITIES_TEST", "Utilities", BY, FIXED);
        repo.save(cat);

        var found = repo.findByCode("UTILITIES_TEST");
        assertThat(found).isPresent();
        assertThat(found.get().getLabel()).isEqualTo("Utilities");
    }

    @Test
    void softFlipFiltersOutInactiveByDefault() {
        var active = ExpenseCategory.create("A_TEST", "Active", BY, FIXED);
        var inactive = ExpenseCategory.create("I_TEST", "Inactive", BY, FIXED);
        repo.save(active);
        repo.save(inactive);
        inactive.setActive(false, BY, FIXED);
        repo.save(inactive);

        var activeOnly = repo.findAllByActive(true);
        assertThat(activeOnly)
                .extracting(ExpenseCategory::getCode)
                .contains("A_TEST")
                .doesNotContain("I_TEST");

        var inactiveOnly = repo.findAllByActive(false);
        assertThat(inactiveOnly)
                .extracting(ExpenseCategory::getCode)
                .contains("I_TEST")
                .doesNotContain("A_TEST");
    }

    @Test
    void uniqueCodeConstraintEnforced() {
        repo.save(ExpenseCategory.create("DUP", "First", BY, FIXED));
        repo.flush();

        var second = ExpenseCategory.create("DUP", "Second", BY, FIXED);
        repo.save(second);
        assertThatThrownBy(repo::flush)
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
