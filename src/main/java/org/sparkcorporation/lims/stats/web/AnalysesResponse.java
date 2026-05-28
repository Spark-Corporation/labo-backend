package org.sparkcorporation.lims.stats.web;

import java.util.List;

import org.sparkcorporation.lims.stats.infra.AnalysisStatRow;

/**
 * JSON response for {@code GET /api/v1/stats/analyses} (spec §5.4): per-analysis resulted, normal
 * and élevé/bas counts, ordered by analysis code in the read model. {@code highCount + lowCount}
 * may be less than the abnormal total because qualitative out-of-range lines have no numeric
 * direction (spec §6.4).
 */
public record AnalysesResponse(List<Row> rows) {

    public record Row(
            String analysisId,
            String code,
            String name,
            long resultedCount,
            long normalCount,
            long highCount,
            long lowCount) {}

    public static AnalysesResponse from(List<AnalysisStatRow> rows) {
        return new AnalysesResponse(
                List.copyOf(
                        rows.stream()
                                .map(
                                        r ->
                                                new Row(
                                                        r.analysisId(),
                                                        r.code(),
                                                        r.name(),
                                                        r.resultedCount(),
                                                        r.normalCount(),
                                                        r.highCount(),
                                                        r.lowCount()))
                                .toList()));
    }
}
