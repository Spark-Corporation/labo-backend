package org.sparkcorporation.lims.result.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Row shape for {@code GET /api/v1/lab-files/queues/overdue} (CDC "Dossiers en retard"). Composes
 * the existing 1.5.b {@link DossierRow} block — re-used unchanged so frontend consumers can share a
 * row component with {@code /queues/urgent} — with an {@link OverdueInfo} sub-block carrying the
 * canary analysis + how-overdue diagnostics.
 *
 * <p>The canary is the analysis with the <strong>earliest deadline</strong> on this lab file
 * (tiebreaker: {@code analysisCode} ASC). It is a read-time snapshot — if a subsequent edit shifts
 * analyses' delays the canary may flip; consumers must not persist it as a stable identifier.
 */
@Schema(description = "A dossier that has at least one analysis past its result deadline.")
public record OverdueDossierRow(
        @Schema(description = "The underlying dossier row.") DossierRow dossier,
        @Schema(description = "Diagnostics about the most-overdue analysis on this dossier.")
                OverdueInfo overdue) {

    @Schema(
            description =
                    "The \"canary\" analysis with the earliest deadline on this lab file — a"
                            + " read-time snapshot, not a stable identifier.")
    public record OverdueInfo(
            @Schema(description = "Code of the canary analysis.") String analysisCode,
            @Schema(description = "Name of the canary analysis.") String analysisName,
            @Schema(description = "Result deadline for the canary analysis.") Instant deadline,
            @Schema(description = "How many hours past deadline the canary analysis is.")
                    int hoursPastDeadline,
            @Schema(description = "Number of overdue analyses on this dossier.")
                    int overdueAnalysisCount) {}
}
