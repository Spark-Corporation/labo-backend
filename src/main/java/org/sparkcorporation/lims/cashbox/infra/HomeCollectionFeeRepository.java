package org.sparkcorporation.lims.cashbox.infra;

import java.time.Instant;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeRecord;

public interface HomeCollectionFeeRepository
        extends JpaRepository<HomeCollectionFeeRecord, String> {

    @Query(
            """
            SELECT f FROM HomeCollectionFeeRecord f
             WHERE (CAST(:from AS java.time.Instant) IS NULL OR f.recordedAt >= :from)
               AND (CAST(:to AS java.time.Instant) IS NULL OR f.recordedAt < :to)
               AND (:labFileId IS NULL OR f.labFileId = :labFileId)
               AND (:recordedBy IS NULL OR f.recordedBy = :recordedBy)
               AND (:cashRegisterId IS NULL OR f.cashRegisterId = :cashRegisterId)
             ORDER BY f.recordedAt DESC, f.id DESC
            """)
    Page<HomeCollectionFeeRecord> search(
            @Param("from") @Nullable Instant from,
            @Param("to") @Nullable Instant to,
            @Param("labFileId") @Nullable String labFileId,
            @Param("recordedBy") @Nullable String recordedBy,
            @Param("cashRegisterId") @Nullable String cashRegisterId,
            Pageable pageable);
}
