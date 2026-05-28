package org.sparkcorporation.lims.stats.web;

import java.util.List;

import org.sparkcorporation.lims.stats.infra.PrescriberStatRow;

/**
 * JSON response for {@code GET /api/v1/stats/by-prescriber} (spec §5.3): per-prescriber dossier
 * counts, ordered by descending count then name in the read model. Dossiers without a prescriber
 * are deliberately omitted (they cannot be attributed to one — spec §5, plan task 2.2).
 */
public record ByPrescriberResponse(List<Row> rows) {

    public record Row(String prescriberId, String prescriberName, long dossierCount) {}

    public static ByPrescriberResponse from(List<PrescriberStatRow> rows) {
        return new ByPrescriberResponse(
                List.copyOf(
                        rows.stream()
                                .map(r -> new Row(r.prescriberId(), r.name(), r.dossierCount()))
                                .toList()));
    }
}
