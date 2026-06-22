package org.sparkcorporation.lims.order.infra;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.order.domain.Sample;

/**
 * Spring Data JPA repository for the sealed {@link Sample} hierarchy (single-table inheritance with
 * {@code sample_type} discriminator).
 *
 * <p>{@link #findByLabFileId(String)} is the second round-trip of the multi-bag-fetch split — see
 * {@link LabFileRepository} class-doc and spec §6.3.
 */
public interface SampleRepository extends JpaRepository<Sample, String> {

    List<Sample> findByLabFileId(String labFileId);

    /**
     * Destruction-overdue queue (CDC Onglet « Conservation »): samples whose computed destruction
     * deadline has passed and that are not yet recorded as destroyed. Ordered by deadline ascending
     * (most overdue first). Backed by the partial index {@code idx_samples_destruction_due}.
     */
    @Query(
            value =
                    """
                    SELECT s.id                         AS "sampleId",
                           s.daily_id                   AS "dailyId",
                           a.code                       AS "analysisCode",
                           a.name                       AS "analysisName",
                           s.portoir                    AS "portoir",
                           s.conservation_start         AS "conservationStart",
                           s.destruction_due_at         AS "destructionDueAt",
                           CEIL(EXTRACT(EPOCH FROM (:now - s.destruction_due_at)) / 86400)::int
                                                        AS "daysOverdue",
                           lf.id                        AS "labFileId",
                           lf.registration_number       AS "labFileReference"
                    FROM samples s
                    JOIN analyses a   ON a.id = s.analysis_id
                    JOIN lab_files lf ON lf.id = s.lab_file_id
                    WHERE s.destroyed_at IS NULL
                      AND s.destruction_due_at IS NOT NULL
                      AND s.destruction_due_at < :now
                    ORDER BY s.destruction_due_at ASC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM samples s
                    WHERE s.destroyed_at IS NULL
                      AND s.destruction_due_at IS NOT NULL
                      AND s.destruction_due_at < :now
                    """,
            nativeQuery = true)
    Page<SampleDestructionQueueProjection> findDestructionDue(
            @Param("now") Instant now, Pageable pageable);

    /**
     * Overdue-and-not-yet-alerted samples for the scheduled destruction-alert scan. Returns
     * entities (not a projection) so the scanner can stamp {@code alertedAt} for per-sample alert
     * dedup.
     */
    @Query(
            """
            SELECT s FROM Sample s
            WHERE s.destroyedAt IS NULL
              AND s.alertedAt IS NULL
              AND s.destructionDueAt IS NOT NULL
              AND s.destructionDueAt < :now
            ORDER BY s.destructionDueAt ASC
            """)
    List<Sample> findOverdueUnalerted(@Param("now") Instant now);
}
