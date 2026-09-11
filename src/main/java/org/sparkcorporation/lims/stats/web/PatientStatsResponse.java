package org.sparkcorporation.lims.stats.web;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.stats.infra.PatientStatsRow;

/**
 * JSON response for {@code GET /api/v1/stats/patients} (spec §5.6): the distinct-patient cohort
 * split ({@code allCount}/{@code newCount}/{@code returningCount}) over dossiers registered in the
 * range, plus the {@code bySex} (patient grain) and {@code byAgeBand} (result-category-snapshot
 * grain) breakdowns (spec §6.6). {@code returningCount = allCount - newCount} (spec §6.3).
 */
@Schema(description = "Patient demographics recap over the requested date range.")
public record PatientStatsResponse(
        @Schema(description = "Distinct-patient counts keyed by sex ('M'/'F').")
                Map<String, Long> bySex,
        @Schema(
                        description =
                                "Distinct-patient counts keyed by age band (patient category"
                                        + " snapshot).")
                Map<String, Long> byAgeBand,
        @Schema(
                        description =
                                "Number of distinct patients whose own account was created inside"
                                        + " the range.")
                long newCount,
        @Schema(description = "allCount minus newCount: patients already known before the range.")
                long returningCount,
        @Schema(
                        description =
                                "Total number of distinct patients across dossiers registered in"
                                        + " the range.")
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
