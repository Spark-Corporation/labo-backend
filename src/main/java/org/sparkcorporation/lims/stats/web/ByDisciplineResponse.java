package org.sparkcorporation.lims.stats.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.stats.infra.DisciplineStatRow;

/**
 * JSON response for {@code GET /api/v1/stats/by-discipline} (spec §5.2): per-discipline resulted
 * and abnormal line counts, ordered by discipline name in the read model.
 */
@Schema(description = "Per-discipline result-line breakdown over the requested date range.")
public record ByDisciplineResponse(
        @Schema(description = "One row per discipline, ordered by discipline name.")
                List<Row> rows) {

    @Schema(description = "Result-line counts for one discipline.")
    public record Row(
            @Schema(description = "ULID of the discipline.") String disciplineId,
            @Schema(description = "Display name of the discipline.") String disciplineName,
            @Schema(
                            description =
                                    "Total number of result lines whose component analysis belongs"
                                            + " to this discipline.")
                    long resultCount,
            @Schema(description = "Number of those lines that are out of range.")
                    long abnormalCount) {}

    public static ByDisciplineResponse from(List<DisciplineStatRow> rows) {
        return new ByDisciplineResponse(
                List.copyOf(
                        rows.stream()
                                .map(
                                        r ->
                                                new Row(
                                                        r.disciplineId(),
                                                        r.name(),
                                                        r.resultCount(),
                                                        r.abnormalCount()))
                                .toList()));
    }
}
