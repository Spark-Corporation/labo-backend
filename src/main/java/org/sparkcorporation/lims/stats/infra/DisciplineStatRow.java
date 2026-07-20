package org.sparkcorporation.lims.stats.infra;

/**
 * Native-SQL projection for {@code GET /api/v1/stats/by-discipline} (spec §5.2). One row per
 * discipline of the resulted lines' <em>component</em> analysis (spec §5 "Discipline attribution"),
 * carrying the total resulted-line count and the out-of-range subset.
 */
public record DisciplineStatRow(
        String disciplineId, String code, String name, long resultCount, long abnormalCount) {}
