package org.sparkcorporation.lims.result.infra;

import java.time.Instant;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.result.domain.Result;

/**
 * Read-only Spring Data repository for §X urgent-dossier and §IX dossier-list queries. Backed by
 * native SQL returning {@link DossierRowProjection} — the aggregate root {@link Result} is used
 * only as the entity-type anchor; no {@link Result} rows are returned by these methods.
 */
public interface DossierRepository extends Repository<Result, String> {

    @Query(
            value =
                    """
                    SELECT
                      lf.id          AS "labFileId",
                      lf.patient_id  AS "patientId",
                      CASE
                        WHEN EXISTS (SELECT 1 FROM results r2
                                     WHERE r2.lab_file_id = lf.id
                                       AND r2.deleted_at IS NULL
                                       AND r2.state = 'DRAFT') THEN 'PEN'
                        WHEN EXISTS (SELECT 1 FROM result_lines rl
                                     JOIN results r3 ON r3.id = rl.result_id
                                     WHERE r3.lab_file_id = lf.id
                                       AND r3.deleted_at IS NULL
                                       AND rl.is_out_of_range = true) THEN 'RED'
                        ELSE 'GREEN'
                      END            AS "marker"
                    FROM lab_files lf
                    WHERE lf.deleted_at IS NULL
                      AND lf.urgent = true
                      AND EXISTS (
                          SELECT 1 FROM results r4
                          WHERE r4.lab_file_id = lf.id
                            AND r4.deleted_at IS NULL
                            AND r4.state <> 'BIOLOGICAL_VALIDATED')
                    ORDER BY lf.created_at ASC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM lab_files lf
                    WHERE lf.deleted_at IS NULL
                      AND lf.urgent = true
                      AND EXISTS (
                          SELECT 1 FROM results r4
                          WHERE r4.lab_file_id = lf.id
                            AND r4.deleted_at IS NULL
                            AND r4.state <> 'BIOLOGICAL_VALIDATED')
                    """,
            nativeQuery = true)
    Page<DossierRowProjection> findUrgent(Pageable pageable);

    @Query(
            value =
                    """
                    SELECT
                      lf.id          AS "labFileId",
                      lf.patient_id  AS "patientId",
                      CASE
                        WHEN EXISTS (SELECT 1 FROM results r2
                                     WHERE r2.lab_file_id = lf.id
                                       AND r2.deleted_at IS NULL
                                       AND r2.state = 'DRAFT') THEN 'PEN'
                        WHEN EXISTS (SELECT 1 FROM result_lines rl
                                     JOIN results r3 ON r3.id = rl.result_id
                                     WHERE r3.lab_file_id = lf.id
                                       AND r3.deleted_at IS NULL
                                       AND rl.is_out_of_range = true) THEN 'RED'
                        ELSE 'GREEN'
                      END            AS "marker"
                    FROM lab_files lf
                    WHERE
                      (CAST(:includeDeleted AS boolean) = true
                         OR lf.deleted_at IS NULL)
                      AND (CAST(:deletedOnly AS boolean) IS NULL
                           OR (:deletedOnly = false AND lf.deleted_at IS NULL)
                           OR (:deletedOnly = true  AND lf.deleted_at IS NOT NULL))
                      AND (CAST(:urgent  AS boolean) IS NULL OR lf.urgent = :urgent)
                      AND (CAST(:onlyEmpty AS boolean) IS NULL
                           OR (:onlyEmpty = false)
                           OR (:onlyEmpty = true AND NOT EXISTS (
                                SELECT 1 FROM results re
                                WHERE re.lab_file_id = lf.id
                                  AND re.deleted_at IS NULL
                                  AND re.state <> 'DRAFT')))
                      AND (CAST(:validatedOnly  AS boolean) IS NULL
                           OR (:validatedOnly = false)
                           OR (:validatedOnly = true AND NOT EXISTS (
                                SELECT 1 FROM results rv
                                WHERE rv.lab_file_id = lf.id
                                  AND rv.deleted_at IS NULL
                                  AND rv.state <> 'BIOLOGICAL_VALIDATED')))
                      AND (CAST(:notValidated AS boolean) IS NULL
                           OR (:notValidated = false)
                           OR (:notValidated = true AND EXISTS (
                                SELECT 1 FROM results rn
                                WHERE rn.lab_file_id = lf.id
                                  AND rn.deleted_at IS NULL
                                  AND rn.state <> 'BIOLOGICAL_VALIDATED')))
                      AND (CAST(:inProgress AS boolean) IS NULL
                           OR (:inProgress = false)
                           OR (:inProgress = true AND EXISTS (
                                SELECT 1 FROM results ri
                                WHERE ri.lab_file_id = lf.id
                                  AND ri.deleted_at IS NULL
                                  AND ri.state IN ('ENTERED', 'TECHNICAL_VALIDATED'))))
                      AND (CAST(:patientId AS text) IS NULL OR lf.patient_id = :patientId)
                      AND (CAST(:dateFrom  AS timestamp) IS NULL OR lf.created_at >= :dateFrom)
                      AND (CAST(:dateTo    AS timestamp) IS NULL OR lf.created_at <  :dateTo)
                    ORDER BY lf.registration_number DESC
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM lab_files lf
                    WHERE
                      (CAST(:includeDeleted AS boolean) = true
                         OR lf.deleted_at IS NULL)
                      AND (CAST(:deletedOnly AS boolean) IS NULL
                           OR (:deletedOnly = false AND lf.deleted_at IS NULL)
                           OR (:deletedOnly = true  AND lf.deleted_at IS NOT NULL))
                      AND (CAST(:urgent  AS boolean) IS NULL OR lf.urgent = :urgent)
                      AND (CAST(:onlyEmpty AS boolean) IS NULL
                           OR (:onlyEmpty = false)
                           OR (:onlyEmpty = true AND NOT EXISTS (
                                SELECT 1 FROM results re
                                WHERE re.lab_file_id = lf.id
                                  AND re.deleted_at IS NULL
                                  AND re.state <> 'DRAFT')))
                      AND (CAST(:validatedOnly  AS boolean) IS NULL
                           OR (:validatedOnly = false)
                           OR (:validatedOnly = true AND NOT EXISTS (
                                SELECT 1 FROM results rv
                                WHERE rv.lab_file_id = lf.id
                                  AND rv.deleted_at IS NULL
                                  AND rv.state <> 'BIOLOGICAL_VALIDATED')))
                      AND (CAST(:notValidated AS boolean) IS NULL
                           OR (:notValidated = false)
                           OR (:notValidated = true AND EXISTS (
                                SELECT 1 FROM results rn
                                WHERE rn.lab_file_id = lf.id
                                  AND rn.deleted_at IS NULL
                                  AND rn.state <> 'BIOLOGICAL_VALIDATED')))
                      AND (CAST(:inProgress AS boolean) IS NULL
                           OR (:inProgress = false)
                           OR (:inProgress = true AND EXISTS (
                                SELECT 1 FROM results ri
                                WHERE ri.lab_file_id = lf.id
                                  AND ri.deleted_at IS NULL
                                  AND ri.state IN ('ENTERED', 'TECHNICAL_VALIDATED'))))
                      AND (CAST(:patientId AS text) IS NULL OR lf.patient_id = :patientId)
                      AND (CAST(:dateFrom  AS timestamp) IS NULL OR lf.created_at >= :dateFrom)
                      AND (CAST(:dateTo    AS timestamp) IS NULL OR lf.created_at <  :dateTo)
                    """,
            nativeQuery = true)
    Page<DossierRowProjection> list(
            @Nullable @Param("includeDeleted") Boolean includeDeleted,
            @Nullable @Param("deletedOnly") Boolean deletedOnly,
            @Nullable @Param("urgent") Boolean urgent,
            @Nullable @Param("onlyEmpty") Boolean onlyEmpty,
            @Nullable @Param("validatedOnly") Boolean validatedOnly,
            @Nullable @Param("notValidated") Boolean notValidated,
            @Nullable @Param("inProgress") Boolean inProgress,
            @Nullable @Param("patientId") String patientId,
            @Nullable @Param("dateFrom") Instant dateFrom,
            @Nullable @Param("dateTo") Instant dateTo,
            Pageable pageable);
}
