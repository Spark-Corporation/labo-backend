package org.sparkcorporation.lims.catalog.infra;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Insurance;

/**
 * Spring Data JPA repository for {@link Insurance}. Note: {@code Insurance} carries
 * {@code @SQLRestriction("disabled_at IS NULL")}, so standard finders (e.g. {@link #findById})
 * return empty for disabled rows. The {@code searchNative} query bypasses Hibernate's restriction
 * (it's native SQL) so callers can list disabled rows when {@code disabled=true} is requested.
 */
public interface InsuranceRepository extends JpaRepository<Insurance, String> {

    Optional<Insurance> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Native-SQL search bypassing the entity's {@code @SQLRestriction}. {@code q} matches against
     * {@code code} OR {@code name} (case-insensitive LIKE). {@code disabled = null} → both active
     * and disabled rows; {@code true} → only disabled; {@code false} → only active.
     */
    @Query(
            value =
                    """
                    SELECT * FROM insurances
                    WHERE (:q IS NULL
                           OR LOWER(code) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                           OR LOWER(name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (:disabled IS NULL
                           OR (:disabled = TRUE  AND disabled_at IS NOT NULL)
                           OR (:disabled = FALSE AND disabled_at IS NULL))
                    ORDER BY code
                    """,
            countQuery =
                    """
                    SELECT count(*) FROM insurances
                    WHERE (:q IS NULL
                           OR LOWER(code) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                           OR LOWER(name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (:disabled IS NULL
                           OR (:disabled = TRUE  AND disabled_at IS NOT NULL)
                           OR (:disabled = FALSE AND disabled_at IS NULL))
                    """,
            nativeQuery = true)
    Page<Insurance> searchNative(
            @Param("q") @Nullable String q,
            @Param("disabled") @Nullable Boolean disabled,
            Pageable pageable);
}
