package org.sparkcorporation.lims.result.infra;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.result.domain.Result;

/**
 * CDC §VII per-paillasse worksheet rows. Native query because the discipline filter lives on the
 * order/catalog side ({@code analyses.discipline_id}) — same cross-module-join precedent as {@link
 * DossierRepository#findOverdue}.
 */
public interface PaillasseWorksheetRepository extends Repository<Result, String> {

    @Query(
            value =
                    """
                    SELECT
                      lf.id                     AS "labFileId",
                      lf.registration_number    AS "registrationNumber",
                      lf.patient_id             AS "patientId",
                      r.analysis_code_snapshot  AS "analysisCode",
                      r.analysis_id             AS "analysisId",
                      r.state                   AS "resultState",
                      rl.component_code_snapshot AS "componentCode",
                      rl.position               AS "position",
                      rl.result_type            AS "resultType",
                      rl.value_integer          AS "valueInteger",
                      rl.value_real             AS "valueReal",
                      rl.value_text             AS "valueText",
                      rl.unit1_snapshot         AS "unit1Snapshot",
                      rl.is_out_of_range        AS "isOutOfRange",
                      rl.reference_interval_id  AS "referenceIntervalId",
                      rl.comment_text           AS "commentText"
                    FROM results r
                    JOIN lab_files lf    ON lf.id = r.lab_file_id
                    JOIN analyses a      ON a.id = r.analysis_id
                    JOIN result_lines rl ON rl.result_id = r.id
                    WHERE a.discipline_id = :disciplineId
                      AND r.deleted_at IS NULL
                      AND lf.deleted_at IS NULL
                      AND lf.created_at >= :from
                      AND lf.created_at < :to
                    ORDER BY lf.registration_number ASC, r.analysis_code_snapshot ASC,
                             rl.position ASC
                    """,
            nativeQuery = true)
    List<PaillasseWorksheetRowProjection> findRows(
            @Param("disciplineId") String disciplineId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
