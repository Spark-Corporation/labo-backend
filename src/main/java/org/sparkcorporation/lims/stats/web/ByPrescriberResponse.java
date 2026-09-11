package org.sparkcorporation.lims.stats.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.stats.infra.PrescriberStatRow;

/**
 * JSON response for {@code GET /api/v1/stats/by-prescriber} (spec §5.3): per-prescriber dossier
 * counts, ordered by descending count then name in the read model. Dossiers without a prescriber
 * are deliberately omitted (they cannot be attributed to one — spec §5, plan task 2.2).
 */
@Schema(description = "Per-prescriber dossier counts over the requested date range.")
public record ByPrescriberResponse(
        @Schema(
                        description =
                                "One row per prescriber, ordered by descending dossier count then"
                                        + " name.")
                List<Row> rows) {

    @Schema(description = "Dossier count for one prescriber.")
    public record Row(
            @Schema(description = "ULID of the prescriber.") String prescriberId,
            @Schema(description = "Display name of the prescriber.") String prescriberName,
            @Schema(description = "Number of dossiers referred by this prescriber in the range.")
                    long dossierCount) {}

    public static ByPrescriberResponse from(List<PrescriberStatRow> rows) {
        return new ByPrescriberResponse(
                List.copyOf(
                        rows.stream()
                                .map(r -> new Row(r.prescriberId(), r.name(), r.dossierCount()))
                                .toList()));
    }
}
