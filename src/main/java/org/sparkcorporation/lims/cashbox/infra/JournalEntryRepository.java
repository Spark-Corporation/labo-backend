package org.sparkcorporation.lims.cashbox.infra;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.cashbox.domain.JournalEntry;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, String> {

    /**
     * Idempotent insert. Returns 1 on insert, 0 if (operation_kind, source_id) already exists. Used
     * by all three writers: ExpenseService (chunk 3), HomeCollectionFeeService (chunk 4),
     * JournalEntryListener (chunk 5). The ON CONFLICT clause is the sole idempotency mechanism;
     * callers do not pre-check existence.
     */
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO cashbox_journal_entries
                      (id, operation_kind, source_id, cash_register_id, user_id, lab_file_id,
                       patient_id, payment_mode, amount, occurred_at)
                    VALUES
                      (:id, :operationKind, :sourceId, :cashRegisterId, :userId, :labFileId,
                       :patientId, :paymentMode, :amount, :occurredAt)
                    ON CONFLICT (operation_kind, source_id) DO NOTHING
                    """,
            nativeQuery = true)
    int insertIdempotent(
            @Param("id") String id,
            @Param("operationKind") String operationKind,
            @Param("sourceId") String sourceId,
            @Param("cashRegisterId") String cashRegisterId,
            @Param("userId") String userId,
            @Param("labFileId") @Nullable String labFileId,
            @Param("patientId") @Nullable String patientId,
            @Param("paymentMode") @Nullable String paymentMode,
            @Param("amount") BigDecimal amount,
            @Param("occurredAt") Instant occurredAt);

    @Query(
            value =
                    """
                    WITH all_entries AS (
                      SELECT je.*,
                             CASE WHEN je.operation_kind = 'PAYMENT' THEN
                               SUM(CASE WHEN je.operation_kind = 'PAYMENT' THEN je.amount ELSE 0 END)
                                 OVER (PARTITION BY je.lab_file_id
                                       ORDER BY je.occurred_at, je.id
                                       ROWS UNBOUNDED PRECEDING)
                             ELSE NULL END AS paid_so_far
                      FROM cashbox_journal_entries je
                    )
                    SELECT id,
                           operation_kind   AS "operationKind",
                           source_id        AS "sourceId",
                           cash_register_id AS "cashRegisterId",
                           user_id          AS "userId",
                           lab_file_id      AS "labFileId",
                           patient_id       AS "patientId",
                           payment_mode     AS "paymentMode",
                           amount,
                           occurred_at      AS "occurredAt",
                           paid_so_far      AS "paidSoFar"
                    FROM   all_entries
                    WHERE  (CAST(:registerId AS varchar) IS NULL OR cash_register_id = :registerId)
                      AND  occurred_at >= :from
                      AND  occurred_at <  :to
                      AND  (CAST(:operationKind AS varchar) IS NULL OR operation_kind = :operationKind)
                    ORDER  BY occurred_at, id
                    """,
            countQuery =
                    """
                    SELECT COUNT(*) FROM cashbox_journal_entries
                     WHERE (CAST(:registerId AS varchar) IS NULL OR cash_register_id = :registerId)
                       AND occurred_at >= :from AND occurred_at < :to
                       AND (CAST(:operationKind AS varchar) IS NULL OR operation_kind = :operationKind)
                    """,
            nativeQuery = true)
    Page<JournalEntryRowProjection> findJournalWithPaidSoFar(
            @Param("registerId") @Nullable String registerId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("operationKind") @Nullable String operationKind,
            Pageable pageable);
}
