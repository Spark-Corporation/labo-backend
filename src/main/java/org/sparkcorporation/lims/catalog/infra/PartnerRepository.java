package org.sparkcorporation.lims.catalog.infra;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.Partner;

/**
 * Spring Data JPA repository for {@link Partner}. {@code searchNative} bypasses the
 * {@code @SQLRestriction} for listing disabled rows when explicitly requested.
 */
public interface PartnerRepository extends JpaRepository<Partner, String> {

    Optional<Partner> findByCode(String code);

    boolean existsByCode(String code);

    @Query(
            value =
                    """
                    SELECT * FROM partners
                    WHERE (:q IS NULL
                           OR LOWER(code) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                           OR LOWER(name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (CAST(:kind AS text) IS NULL OR kind = CAST(:kind AS text))
                      AND (:disabled IS NULL
                           OR (:disabled = TRUE  AND disabled_at IS NOT NULL)
                           OR (:disabled = FALSE AND disabled_at IS NULL))
                    ORDER BY code
                    """,
            countQuery =
                    """
                    SELECT count(*) FROM partners
                    WHERE (:q IS NULL
                           OR LOWER(code) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                           OR LOWER(name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                      AND (CAST(:kind AS text) IS NULL OR kind = CAST(:kind AS text))
                      AND (:disabled IS NULL
                           OR (:disabled = TRUE  AND disabled_at IS NOT NULL)
                           OR (:disabled = FALSE AND disabled_at IS NULL))
                    """,
            nativeQuery = true)
    Page<Partner> searchNative(
            @Param("q") @Nullable String q,
            @Param("kind") @Nullable String kind,
            @Param("disabled") @Nullable Boolean disabled,
            Pageable pageable);
}
