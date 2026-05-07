package org.sparkcorporation.lims.catalog.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module read-only iface for the {@code Analysis} aggregate. Other modules import {@code
 * catalog.api} only.
 */
public interface AnalysisApi {

    Optional<AnalysisSummary> findById(AnalysisId id);

    Optional<AnalysisSummary> findByCode(AnalysisCode code);

    /** Throws {@code AnalysisNotFoundException} when no row matches. */
    AnalysisSummary requireById(AnalysisId id);

    boolean existsById(AnalysisId id);

    List<AnalysisSummary> findAllActive();

    /** Used by 1.4 {@code order} workflows to populate analysis pickers per discipline. */
    List<AnalysisSummary> findActiveByDiscipline(DisciplineId disciplineId);
}
