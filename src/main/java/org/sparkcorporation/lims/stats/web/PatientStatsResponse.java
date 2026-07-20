package org.sparkcorporation.lims.stats.web;

import java.util.Map;

import org.sparkcorporation.lims.stats.infra.PatientStatsRow;

/**
 * JSON response for {@code GET /api/v1/stats/patients} (spec §5.6): the distinct-patient cohort
 * split ({@code allCount}/{@code newCount}/{@code returningCount}) over dossiers registered in the
 * range, plus the {@code bySex} (patient grain) and {@code byAgeBand} (result-category-snapshot
 * grain) breakdowns (spec §6.6). {@code returningCount = allCount - newCount} (spec §6.3).
 */
public record PatientStatsResponse(
        Map<String, Long> bySex,
        Map<String, Long> byAgeBand,
        long newCount,
        long returningCount,
        long allCount) {

    public static PatientStatsResponse from(PatientStatsRow r) {
        return new PatientStatsResponse(
                Map.copyOf(r.bySex()),
                Map.copyOf(r.byAgeBand()),
                r.newCount(),
                r.allCount() - r.newCount(),
                r.allCount());
    }
}
