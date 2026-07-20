package org.sparkcorporation.lims.catalog.api;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Cross-module read snapshot of a {@code ReferenceInterval} row — flat record per spec §5.3.
 *
 * <p>Reuses {@link PatientCategory}, {@link Sex}, and {@link AgeUnit} from {@code shared.domain}
 * (already in catalog's allowedDependencies via {@code shared::domain}).
 */
public record ReferenceIntervalSummary(
        ReferenceIntervalId id,
        AnalysisId analysisId,
        PatientCategory category,
        @Nullable Sex sex,
        @Nullable Integer ageMin,
        @Nullable Integer ageMax,
        @Nullable AgeUnit ageUnit,
        ReferenceMode mode,
        @Nullable BigDecimal unit1Min,
        @Nullable BigDecimal unit1Max,
        @Nullable BigDecimal unit2Min,
        @Nullable BigDecimal unit2Max,
        @Nullable String commentText) {}
