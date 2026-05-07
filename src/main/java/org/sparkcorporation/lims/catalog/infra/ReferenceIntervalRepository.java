package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.ReferenceInterval;
import org.sparkcorporation.lims.shared.domain.PatientCategory;

/**
 * Spring Data JPA repository for {@link ReferenceInterval}.
 *
 * <p>The bulk-soft-delete query {@link #softDeleteByAnalysisIdAndCategoryIn} is used by the
 * replicate workflow to invalidate target categories before re-cloning from the source category. It
 * returns the row count so the service can validate "at least one row updated" semantics where
 * required.
 */
public interface ReferenceIntervalRepository extends JpaRepository<ReferenceInterval, String> {

    @Query(
            """
            SELECT ri FROM ReferenceInterval ri
            WHERE ri.analysisId = :analysisId AND ri.deletedAt IS NULL
            ORDER BY ri.category, ri.ageMin NULLS FIRST
            """)
    List<ReferenceInterval> findActiveByAnalysisId(@Param("analysisId") String analysisId);

    @Query(
            """
            SELECT ri FROM ReferenceInterval ri
            WHERE ri.analysisId = :analysisId
              AND ri.category = :category
              AND ri.deletedAt IS NULL
            ORDER BY ri.ageMin NULLS FIRST
            """)
    List<ReferenceInterval> findActiveByAnalysisIdAndCategory(
            @Param("analysisId") String analysisId, @Param("category") PatientCategory category);

    @Modifying
    @Query(
            """
            UPDATE ReferenceInterval ri
            SET ri.deletedAt = CURRENT_TIMESTAMP, ri.version = ri.version + 1
            WHERE ri.analysisId = :analysisId
              AND ri.category IN :categories
              AND ri.deletedAt IS NULL
            """)
    int softDeleteByAnalysisIdAndCategoryIn(
            @Param("analysisId") String analysisId,
            @Param("categories") Set<PatientCategory> categories);
}
