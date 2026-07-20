package org.sparkcorporation.lims.order.infra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.domain.LabFile;

/**
 * Spring Data JPA repository for {@link LabFile}. The generic ID parameter is {@code String} (the
 * raw ULID value); cross-module callers pass {@code LabFileId} into {@link DefaultLabFileApi} which
 * extracts {@code .value()} before delegating here.
 *
 * <p><b>Soft-delete semantics.</b> {@link LabFile} uses {@code @SQLDelete} only (NO
 * {@code @SQLRestriction}, per spec §4.4) so historical FK integrity from past lab-file records is
 * preserved. Active-finders therefore explicitly filter {@code lf.deletedAt IS NULL}.
 *
 * <p><b>Multi-bag fetch avoidance.</b> Hibernate throws {@code MultipleBagFetchException} when JOIN
 * FETCH spans more than one {@code List}-typed collection. {@link LabFile} owns two bags ({@code
 * samples} and {@code analyses}), so {@link #findActiveById(String)} loads the lab-file row only —
 * {@link DefaultLabFileApi} resolves the bags via {@link SampleRepository#findByLabFileId(String)}
 * and {@link AnalysisRequestRepository#findByLabFileId(String)} batched lookups (one extra
 * round-trip per bag, no N+1; see spec §6.3).
 */
public interface LabFileRepository extends JpaRepository<LabFile, String> {

    @Query(
            """
            SELECT lf FROM LabFile lf
             WHERE lf.id = :id AND lf.deletedAt IS NULL
            """)
    Optional<LabFile> findActiveById(@Param("id") String id);

    @Query(
            """
            SELECT lf FROM LabFile lf
             WHERE lf.registrationNumber.value = :rn AND lf.deletedAt IS NULL
            """)
    Optional<LabFile> findActiveByRegistrationNumber(@Param("rn") String rn);

    @Query(
            """
            SELECT lf FROM LabFile lf
             WHERE lf.patientId = :patientId AND lf.deletedAt IS NULL
             ORDER BY lf.createdAt DESC
            """)
    List<LabFile> findActiveByPatientId(@Param("patientId") String patientId);

    @Query(
            """
            SELECT lf FROM LabFile lf
             WHERE lf.id IN :ids AND lf.deletedAt IS NULL
            """)
    List<LabFile> findAllActiveByIds(@Param("ids") Set<String> ids);

    @Query(
            """
            SELECT lf FROM LabFile lf
             WHERE lf.deletedAt IS NULL
               AND (:patientId IS NULL OR lf.patientId = :patientId)
               AND (:prescriberId IS NULL OR lf.prescriberId = :prescriberId)
               AND (:urgent IS NULL OR lf.urgent = :urgent)
               AND (:status IS NULL OR lf.status = :status)
               AND (:from IS NULL OR lf.createdAt >= :from)
               AND (:to IS NULL OR lf.createdAt <  :to)
             ORDER BY lf.createdAt DESC
            """)
    Page<LabFile> search(
            @Param("patientId") @Nullable String patientId,
            @Param("prescriberId") @Nullable String prescriberId,
            @Param("urgent") @Nullable Boolean urgent,
            @Param("status") @Nullable LabFileStatus status,
            @Param("from") @Nullable Instant from,
            @Param("to") @Nullable Instant to,
            Pageable pageable);
}
