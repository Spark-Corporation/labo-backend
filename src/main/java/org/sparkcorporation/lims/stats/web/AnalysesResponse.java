package org.sparkcorporation.lims.stats.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.stats.infra.AnalysisStatRow;

/**
 * JSON response for {@code GET /api/v1/stats/analyses} (spec §5.4): per-analysis resulted, normal
 * and élevé/bas counts, ordered by analysis code in the read model. {@code highCount + lowCount}
 * may be less than the abnormal total because qualitative out-of-range lines have no numeric
 * direction (spec §6.4).
 */
@Schema(description = "Per-analysis result-line breakdown over the requested date range.")
public record AnalysesResponse(
        @Schema(description = "One row per analysis, ordered by analysis code.") List<Row> rows) {

    @Schema(description = "Resulted-line counts for one analysis.")
    public record Row(
            @Schema(description = "ULID of the analysis.") String analysisId,
            @Schema(description = "Short code of the analysis.") String code,
            @Schema(description = "Display name of the analysis.") String name,
            @Schema(description = "Total number of result lines for this analysis.")
                    long resultedCount,
            @Schema(description = "Number of those lines within the reference interval.")
                    long normalCount,
            @Schema(
                            description =
                                    "Number of out-of-range lines above the reference interval's"
                                        + " upper bound (numeric, STANDARD-mode intervals only).")
                    long highCount,
            @Schema(
                            description =
                                    "Number of out-of-range lines below the reference interval's"
                                        + " lower bound (numeric, STANDARD-mode intervals only).")
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
