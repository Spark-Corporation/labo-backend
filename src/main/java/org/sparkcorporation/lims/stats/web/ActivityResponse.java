package org.sparkcorporation.lims.stats.web;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.stats.infra.ActivityRow;

/**
 * JSON response for {@code GET /api/v1/stats/activity} (spec §5.1). Mirrors {@link ActivityRow}
 * with the derived {@code patientsReturning = patientsAll - patientsNew} cohort count (spec §6.3).
 */
@Schema(description = "Activity recap over dossiers registered in the requested date range.")
public record ActivityResponse(
        @Schema(description = "Total number of dossiers registered in the range.") long dossiers,
        @Schema(description = "Number of those dossiers flagged urgent.") long urgents,
        @Schema(description = "Number of those dossiers biologically validated or published.")
                long validated,
        @Schema(description = "Number of distinct patients across those dossiers.")
                long patientsAll,
        @Schema(
                        description =
                                "Number of those patients whose own account was created inside the"
                                        + " range.")
                long patientsNew,
        @Schema(
                        description =
                                "patientsAll minus patientsNew: patients already known before the"
                                        + " range.")
                long patientsReturning) {

    public static ActivityResponse from(ActivityRow r) {
        return new ActivityResponse(
                r.dossiers(),
                r.urgents(),
                r.validated(),
                r.patientsAll(),
                r.patientsNew(),
                r.patientsAll() - r.patientsNew());
    }
}
