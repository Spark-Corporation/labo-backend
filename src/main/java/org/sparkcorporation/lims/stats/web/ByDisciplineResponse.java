package org.sparkcorporation.lims.stats.web;

import java.util.List;

import org.sparkcorporation.lims.stats.infra.DisciplineStatRow;

/**
 * JSON response for {@code GET /api/v1/stats/by-discipline} (spec §5.2): per-discipline resulted
 * and abnormal line counts, ordered by discipline name in the read model.
 */
public record ByDisciplineResponse(List<Row> rows) {

    public record Row(
            String disciplineId, String disciplineName, long resultCount, long abnormalCount) {}

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
