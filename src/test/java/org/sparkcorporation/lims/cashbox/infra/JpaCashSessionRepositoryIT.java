package org.sparkcorporation.lims.cashbox.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.JournalEntryId;
import org.sparkcorporation.lims.cashbox.api.OperationKind;
import org.sparkcorporation.lims.cashbox.api.SessionStatus;
import org.sparkcorporation.lims.cashbox.domain.CashSession;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaCashSessionRepositoryIT extends IntegrationTestBase {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);
    private static final UserId BY = UserId.next();

    @Autowired private CashSessionRepository repo;
    @Autowired private JournalEntryRepository journalRepo;

    @Test
    void compositeIdRoundTrip() {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");
        repo.save(CashSession.openIfMissing(register, date));

        var found = repo.findByCashRegisterIdAndSessionDate(register.value(), date);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(SessionStatus.OPEN);
        assertThat(found.get().getSessionDate()).isEqualTo(date);
    }

    @Test
    void computeTotalsAggregatesPerMode() {
        var register = CashRegisterId.generate();
        var date = LocalDate.parse("2026-05-18");
        var occurredAt = Instant.parse("2026-05-18T10:00:00Z");

        journalRepo.insertIdempotent(
                JournalEntryId.generate().value(),
                OperationKind.PAYMENT.name(),
                Ulid.generate(),
                register.value(),
                BY.value(),
                Ulid.generate(),
                null,
                "CASH",
                new BigDecimal("10000"),
                occurredAt);
        journalRepo.insertIdempotent(
                JournalEntryId.generate().value(),
                OperationKind.EXPENSE.name(),
                Ulid.generate(),
                register.value(),
                BY.value(),
                null,
                null,
                "CASH",
                new BigDecimal("2000"),
                occurredAt);
        journalRepo.insertIdempotent(
                JournalEntryId.generate().value(),
                OperationKind.HOME_COLLECTION_FEE.name(),
                Ulid.generate(),
                register.value(),
                BY.value(),
                Ulid.generate(),
                Ulid.generate(),
                null,
                new BigDecimal("3000"),
                occurredAt);

        var totals = repo.computeTotals(register.value(), date);

        assertThat(totals.getCash()).isEqualByComparingTo("10000");
        assertThat(totals.getCheque()).isEqualByComparingTo("0");
        assertThat(totals.getCardTpe()).isEqualByComparingTo("0");
        assertThat(totals.getFlooz()).isEqualByComparingTo("0");
        assertThat(totals.getMixxByYas()).isEqualByComparingTo("0");
        assertThat(totals.getExpenses()).isEqualByComparingTo("2000");
        assertThat(totals.getHomeCollection()).isEqualByComparingTo("3000");
    }
}
