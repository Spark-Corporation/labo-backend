package org.sparkcorporation.lims.cashbox.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.cashbox.api.JournalEntryId;
import org.sparkcorporation.lims.cashbox.api.OperationKind;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaJournalEntryRepositoryIT extends IntegrationTestBase {

    @Autowired private JournalEntryRepository repo;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void insertIdempotentReturns1OnFirst0OnDuplicate() {
        var sourceId = Ulid.generate();
        var cashRegisterId = Ulid.generate();
        var userId = Ulid.generate();
        var occurredAt = Instant.parse("2026-05-18T10:00:00Z");

        int first =
                repo.insertIdempotent(
                        JournalEntryId.generate().value(),
                        OperationKind.EXPENSE.name(),
                        sourceId,
                        cashRegisterId,
                        userId,
                        null,
                        null,
                        "CASH",
                        new BigDecimal("100.00"),
                        occurredAt);
        int second =
                repo.insertIdempotent(
                        JournalEntryId.generate().value(),
                        OperationKind.EXPENSE.name(),
                        sourceId,
                        cashRegisterId,
                        userId,
                        null,
                        null,
                        "CASH",
                        new BigDecimal("999.99"),
                        occurredAt);

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);

        Integer rows =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cashbox_journal_entries WHERE operation_kind = ? AND"
                                + " source_id = ?",
                        Integer.class,
                        "EXPENSE",
                        sourceId);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void paidSoFarIsCorrectEvenWhenInsertOrderDoesNotMatchOccurredAtOrder() {
        var labFile = Ulid.generate();
        var register = Ulid.generate();
        var user = Ulid.generate();

        insertPaymentJournalRow(
                labFile,
                register,
                user,
                Instant.parse("2026-05-18T12:00:00Z"),
                new BigDecimal("5000"));
        insertPaymentJournalRow(
                labFile,
                register,
                user,
                Instant.parse("2026-05-18T10:00:00Z"),
                new BigDecimal("10000"));
        insertPaymentJournalRow(
                labFile,
                register,
                user,
                Instant.parse("2026-05-18T11:00:00Z"),
                new BigDecimal("7000"));

        var page =
                repo.findJournalWithPaidSoFar(
                        null,
                        Instant.parse("2026-05-18T09:00:00Z"),
                        Instant.parse("2026-05-18T13:00:00Z"),
                        null,
                        PageRequest.of(0, 50));

        var labFileRows =
                page.getContent().stream().filter(r -> labFile.equals(r.getLabFileId())).toList();
        assertThat(labFileRows).hasSize(3);
        assertThat(labFileRows.get(0).getPaidSoFar()).isEqualByComparingTo("10000");
        assertThat(labFileRows.get(1).getPaidSoFar()).isEqualByComparingTo("17000");
        assertThat(labFileRows.get(2).getPaidSoFar()).isEqualByComparingTo("22000");
    }

    private void insertPaymentJournalRow(
            String labFileId,
            String cashRegisterId,
            String userId,
            Instant occurredAt,
            BigDecimal amount) {
        repo.insertIdempotent(
                JournalEntryId.generate().value(),
                OperationKind.PAYMENT.name(),
                Ulid.generate(),
                cashRegisterId,
                userId,
                labFileId,
                null,
                "CASH",
                amount,
                occurredAt);
    }

    @Test
    void checkConstraintBlocksExpenseWithoutPaymentMode() {
        var id = Ulid.generate();
        var sourceId = Ulid.generate();
        var cashRegisterId = Ulid.generate();
        var userId = Ulid.generate();
        var occurredAt = Timestamp.from(Instant.parse("2026-05-18T10:00:00Z"));

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO cashbox_journal_entries (id, operation_kind,"
                                            + " source_id, cash_register_id, user_id, lab_file_id,"
                                            + " patient_id, payment_mode, amount, occurred_at)"
                                            + " VALUES (?, 'EXPENSE', ?, ?, ?, NULL, NULL, NULL,"
                                            + " 100.00, ?)",
                                        id,
                                        sourceId,
                                        cashRegisterId,
                                        userId,
                                        occurredAt))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
