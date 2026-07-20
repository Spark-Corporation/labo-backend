package org.sparkcorporation.lims.stats.infra;

/**
 * Native-SQL projection for {@code GET /api/v1/stats/activity} (spec §5.1). Carries the raw
 * aggregate counts; {@code patientsReturning} is derived downstream as {@code patientsAll -
 * patientsNew}.
 */
public record ActivityRow(
        long dossiers, long urgents, long validated, long patientsAll, long patientsNew) {}
