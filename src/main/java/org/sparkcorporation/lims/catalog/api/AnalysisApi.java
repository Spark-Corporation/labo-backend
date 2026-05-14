package org.sparkcorporation.lims.catalog.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    /**
     * Batched lookup. Returns a map keyed by the input ids; missing or inactive ids are silently
     * absent. Empty input → empty map. Added in 1.6.a for {@code billing} invoice finalization to
     * resolve all analysis snapshots on a lab-file with a single query.
     */
    Map<AnalysisId, AnalysisSummary> findAllByIds(Collection<AnalysisId> ids);

    /**
     * Returns all <strong>active</strong> {@link ReferenceIntervalSummary} rows for an analysis
     * (filters {@code deleted_at IS NULL}). Soft-deleted RIs are excluded — matching the existing
     * {@code ReferenceIntervalRepository.findActiveByAnalysisId} semantics and the catalog admin
     * UI.
     *
     * <p>Out-of-range scoring on result lines uses only RIs that are still active at scoring time;
     * the matched {@code ReferenceIntervalId} is then frozen on the line per result spec §5.4 (no
     * retroactive recomputation).
     *
     * @since 1.5.a
     */
    List<ReferenceIntervalSummary> findActiveReferenceIntervalsByAnalysis(AnalysisId analysisId);
}
