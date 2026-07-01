package org.sparkcorporation.lims.catalog.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.sparkcorporation.lims.catalog.domain.ReferenceInterval;
import org.sparkcorporation.lims.shared.domain.PatientCategory;

/** Spring Data JPA repository for {@link ReferenceInterval}. */
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
}
