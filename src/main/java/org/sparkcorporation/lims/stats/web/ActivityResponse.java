package org.sparkcorporation.lims.stats.web;

import org.sparkcorporation.lims.stats.infra.ActivityRow;

/**
 * JSON response for {@code GET /api/v1/stats/activity} (spec §5.1). Mirrors {@link ActivityRow}
 * with the derived {@code patientsReturning = patientsAll - patientsNew} cohort count (spec §6.3).
 */
public record ActivityResponse(
        long dossiers,
        long urgents,
        long validated,
        long patientsAll,
        long patientsNew,
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
