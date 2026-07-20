package org.sparkcorporation.lims.result.infra;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.result.domain.Result;

/**
 * §V line-level search repository — native query joining {@code results} + {@code result_lines} +
 * {@code lab_files}, with optional filters and Pageable pagination. Soft-deleted rows excluded
 * explicitly (1.5.a uses {@code @SQLDelete} without {@code @SQLRestriction}).
 *
 * <p>The native query uses a custom {@code countQuery} that strips the column projection but
 * preserves the same FROM/WHERE.
 *
 * <p>{@code insuredFilter} keys off {@code patients.insurance_id}; {@code partnerKindEq} keys off
 * {@code partners.kind} (joined via {@code patients.partner_id}). The two are mutually exclusive at
 * the service layer — {@link org.sparkcorporation.lims.result.web.ResultSearchService} only sets
 * one based on the §V {@code patientType} enum.
 */
public interface ResultSearchRepository extends Repository<Result, String> {

    @Query(
            value =
                    """
                    SELECT
                      r.id                              AS "resultId",
                      rl.id                             AS "lineId",
                      r.lab_file_id                     AS "labFileId",
                      lf.registration_number            AS "labFileRegistrationNumber",
                      lf.patient_id                     AS "patientId",
                      r.analysis_code_snapshot          AS "analysisCode",
                      rl.component_code_snapshot        AS "componentCode",
                      rl.position                       AS "position",
                      rl.result_type                    AS "resultType",
                      rl.value_integer                  AS "valueInteger",
                      rl.value_real                     AS "valueReal",
                      rl.value_text                     AS "valueText",
                      rl.unit1_snapshot                 AS "unit1Snapshot",
                      rl.is_out_of_range                AS "outOfRange",
                      rl.reference_interval_id          AS "referenceIntervalId",
                      r.state                           AS "resultState",
                      r.biological_validated_at         AS "biologicalValidatedAt",
                      r.created_at                      AS "createdAt"
                    FROM results r
                    JOIN result_lines rl ON rl.result_id = r.id
                    JOIN lab_files    lf ON lf.id        = r.lab_file_id
                    WHERE r.deleted_at IS NULL
                      AND lf.deleted_at IS NULL
                      AND (CAST(:dateFrom    AS timestamp) IS NULL OR lf.created_at >= :dateFrom)
                      AND (CAST(:dateTo      AS timestamp) IS NULL OR lf.created_at <  :dateTo)
                      AND (CAST(:analysisCode AS text)     IS NULL
                           OR r.analysis_code_snapshot   = :analysisCode
                           OR rl.component_code_snapshot = :analysisCode)
                      AND (CAST(:insuredFilter AS boolean) IS NULL
                           OR (CAST(:insuredFilter AS boolean) = true AND EXISTS (
                                SELECT 1 FROM patients p
                                WHERE p.id = lf.patient_id
                                  AND p.insurance_id IS NOT NULL))
                           OR (CAST(:insuredFilter AS boolean) = false AND EXISTS (
                                SELECT 1 FROM patients p
                                WHERE p.id = lf.patient_id
                                  AND p.insurance_id IS NULL
                                  AND p.partner_id IS NULL)))
                      AND (CAST(:partnerKindEq AS text) IS NULL
                           OR EXISTS (
                                SELECT 1 FROM patients p2
                                JOIN partners pt ON pt.id = p2.partner_id
                                WHERE p2.id = lf.patient_id
                                  AND pt.kind = :partnerKindEq))
                      AND (CAST(:stateEq AS text) IS NULL OR r.state = :stateEq)
                      AND (CAST(:notValidated AS boolean) IS NULL
                           OR r.state IN ('DRAFT','ENTERED','TECHNICAL_VALIDATED'))
                      AND (CAST(:urgentOnly  AS boolean) IS NULL OR lf.urgent = true)
                    ORDER BY r.biological_validated_at DESC NULLS LAST, r.created_at DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM results r
                    JOIN result_lines rl ON rl.result_id = r.id
                    JOIN lab_files    lf ON lf.id        = r.lab_file_id
                    WHERE r.deleted_at IS NULL
                      AND lf.deleted_at IS NULL
                      AND (CAST(:dateFrom    AS timestamp) IS NULL OR lf.created_at >= :dateFrom)
                      AND (CAST(:dateTo      AS timestamp) IS NULL OR lf.created_at <  :dateTo)
                      AND (CAST(:analysisCode AS text)     IS NULL
                           OR r.analysis_code_snapshot   = :analysisCode
                           OR rl.component_code_snapshot = :analysisCode)
                      AND (CAST(:insuredFilter AS boolean) IS NULL
                           OR (CAST(:insuredFilter AS boolean) = true AND EXISTS (
                                SELECT 1 FROM patients p
                                WHERE p.id = lf.patient_id
                                  AND p.insurance_id IS NOT NULL))
                           OR (CAST(:insuredFilter AS boolean) = false AND EXISTS (
                                SELECT 1 FROM patients p
                                WHERE p.id = lf.patient_id
                                  AND p.insurance_id IS NULL
                                  AND p.partner_id IS NULL)))
                      AND (CAST(:partnerKindEq AS text) IS NULL
                           OR EXISTS (
                                SELECT 1 FROM patients p2
                                JOIN partners pt ON pt.id = p2.partner_id
                                WHERE p2.id = lf.patient_id
                                  AND pt.kind = :partnerKindEq))
                      AND (CAST(:stateEq AS text) IS NULL OR r.state = :stateEq)
                      AND (CAST(:notValidated AS boolean) IS NULL
                           OR r.state IN ('DRAFT','ENTERED','TECHNICAL_VALIDATED'))
                      AND (CAST(:urgentOnly  AS boolean) IS NULL OR lf.urgent = true)
                    """,
            nativeQuery = true)
    Page<ResultSearchRowProjection> search(
            @Param("dateFrom") @Nullable Instant dateFrom,
            @Param("dateTo") @Nullable Instant dateTo,
            @Param("analysisCode") @Nullable String analysisCode,
            @Param("insuredFilter") @Nullable Boolean insuredFilter,
            @Param("partnerKindEq") @Nullable String partnerKindEq,
            @Param("stateEq") @Nullable String stateEq,
            @Param("notValidated") @Nullable Boolean notValidated,
            @Param("urgentOnly") @Nullable Boolean urgentOnly,
            Pageable pageable);

    static Instant endOfDay(LocalDate date) {
        return LocalTime.MAX.atDate(date).toInstant(ZoneOffset.UTC);
    }

    static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
