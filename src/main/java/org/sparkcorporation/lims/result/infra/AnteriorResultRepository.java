package org.sparkcorporation.lims.result.infra;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.result.domain.Result;

/**
 * §IV anterior-results lookup — native query returning the most recent {@code BIOLOGICAL_VALIDATED}
 * result lines for a given patient and component code. Bounded by {@code LIMIT :limit} (server-side
 * clamped to [1, 12] by the service layer).
 *
 * <p>NOT paginated — returns a bounded list directly.
 */
public interface AnteriorResultRepository extends Repository<Result, String> {

    @Query(
            value =
                    """
                    SELECT
                      r.id                          AS "resultId",
                      rl.id                         AS "lineId",
                      lf.id                         AS "labFileId",
                      lf.registration_number        AS "labFileRegistrationNumber",
                      rl.component_code_snapshot    AS "componentCode",
                      rl.result_type                AS "resultType",
                      rl.value_integer              AS "valueInteger",
                      rl.value_real                 AS "valueReal",
                      rl.value_text                 AS "valueText",
                      rl.unit1_snapshot             AS "unit1Snapshot",
                      rl.is_out_of_range            AS "outOfRange",
                      r.biological_validated_at     AS "biologicalValidatedAt"
                    FROM results r
                    JOIN result_lines rl ON rl.result_id = r.id
                    JOIN lab_files    lf ON lf.id        = r.lab_file_id
                    WHERE r.deleted_at IS NULL
                      AND lf.deleted_at IS NULL
                      AND r.state = 'BIOLOGICAL_VALIDATED'
                      AND lf.patient_id = :patientId
                      AND rl.component_code_snapshot = :componentCode
                    ORDER BY r.biological_validated_at DESC
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<AnteriorResultRowProjection> findHistory(
            @Param("patientId") String patientId,
            @Param("componentCode") String componentCode,
            @Param("limit") int limit);
}
