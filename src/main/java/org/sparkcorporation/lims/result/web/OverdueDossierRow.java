package org.sparkcorporation.lims.result.web;

import java.time.Instant;

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
public record OverdueDossierRow(DossierRow dossier, OverdueInfo overdue) {

    public record OverdueInfo(
            String analysisCode,
            String analysisName,
            Instant deadline,
            int hoursPastDeadline,
            int overdueAnalysisCount) {}
}
