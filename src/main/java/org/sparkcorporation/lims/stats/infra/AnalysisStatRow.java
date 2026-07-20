package org.sparkcorporation.lims.stats.infra;

/**
 * Native-SQL projection for {@code GET /api/v1/stats/analyses} (spec §5.4). One row per
 * <em>component</em> analysis of the resulted lines, carrying the total resulted-line count, the
 * in-range (normal) subset, and the élevé/bas split. {@code highCount}/{@code lowCount} are derived
 * only from out-of-range lines backed by a numeric {@code STANDARD}-mode reference interval; a
 * qualitative out-of-range line contributes to {@code resultedCount} (and the abnormal total) but
 * to neither direction, so {@code highCount + lowCount ≤ abnormal} by design (spec §6.4).
 */
public record AnalysisStatRow(
        String analysisId,
        String code,
        String name,
        long resultedCount,
        long normalCount,
        long highCount,
        long lowCount) {}
