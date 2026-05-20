package org.sparkcorporation.lims.cashbox.infra;

import java.time.Instant;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.cashbox.domain.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, String> {

    @Query(
            """
            SELECT e FROM Expense e
             WHERE (CAST(:from AS java.time.Instant) IS NULL OR e.recordedAt >= :from)
               AND (CAST(:to AS java.time.Instant) IS NULL OR e.recordedAt < :to)
               AND (:categoryId IS NULL OR e.categoryId = :categoryId)
               AND (:recordedBy IS NULL OR e.recordedBy = :recordedBy)
               AND (:cashRegisterId IS NULL OR e.cashRegisterId = :cashRegisterId)
             ORDER BY e.recordedAt DESC, e.id DESC
            """)
    Page<Expense> search(
            @Param("from") @Nullable Instant from,
            @Param("to") @Nullable Instant to,
            @Param("categoryId") @Nullable String categoryId,
            @Param("recordedBy") @Nullable String recordedBy,
            @Param("cashRegisterId") @Nullable String cashRegisterId,
            Pageable pageable);
}
