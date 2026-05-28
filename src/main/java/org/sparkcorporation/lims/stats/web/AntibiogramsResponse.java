package org.sparkcorporation.lims.stats.web;

import java.util.List;

import org.sparkcorporation.lims.stats.infra.AntibiogramStatRow;

/**
 * JSON response for {@code GET /api/v1/stats/antibiograms} (spec §5.5): per-antibiotic S/I/R bucket
 * counts, ordered by antibiotic name in the read model.
 */
public record AntibiogramsResponse(List<Row> rows) {

    public record Row(
            String antibioticId,
            String antibioticName,
            long sensible,
            long intermediate,
            long resistant) {}

    public static AntibiogramsResponse from(List<AntibiogramStatRow> rows) {
        return new AntibiogramsResponse(
                List.copyOf(
                        rows.stream()
                                .map(
                                        r ->
                                                new Row(
                                                        r.antibioticId(),
                                                        r.name(),
                                                        r.sensible(),
                                                        r.intermediate(),
                                                        r.resistant()))
                                .toList()));
    }
}
