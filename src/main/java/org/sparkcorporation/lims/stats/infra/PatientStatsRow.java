package org.sparkcorporation.lims.stats.infra;

import java.util.Map;

/**
 * Aggregate carrier for {@code GET /api/v1/stats/patients} (spec §5.6, §6.6). Combines the two
 * grains the read model queries separately: the patient-grain cohort and sex split ({@code
 * allCount}/{@code newCount} and {@code bySex}) computed over {@code lab_files} ⋈ {@code patients},
 * and the {@code byAgeBand} distinct-patient counts per {@code results.patient_category_snapshot}.
 */
public record PatientStatsRow(
        long allCount, long newCount, Map<String, Long> bySex, Map<String, Long> byAgeBand) {}
