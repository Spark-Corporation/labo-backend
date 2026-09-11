package org.sparkcorporation.lims.stats.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.stats.infra.AntibiogramStatRow;

/**
 * JSON response for {@code GET /api/v1/stats/antibiograms} (spec §5.5): per-antibiotic S/I/R bucket
 * counts, ordered by antibiotic name in the read model.
 */
@Schema(description = "Per-antibiotic sensitivity breakdown over the requested date range.")
public record AntibiogramsResponse(
        @Schema(description = "One row per antibiotic, ordered by antibiotic name.")
                List<Row> rows) {

    @Schema(description = "Sensitivity reading counts for one antibiotic.")
    public record Row(
            @Schema(description = "ULID of the antibiotic.") String antibioticId,
            @Schema(description = "Display name of the antibiotic.") String antibioticName,
            @Schema(description = "Number of readings interpreted as Sensible (S).") long sensible,
            @Schema(description = "Number of readings interpreted as Intermediate (I).")
                    long intermediate,
            @Schema(description = "Number of readings interpreted as Resistant (R).")
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
