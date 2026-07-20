package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Command to create a {@code ReferenceInterval}. Validation per mode is enforced by {@link
 * ReferenceInterval#create(NewReferenceInterval, org.sparkcorporation.lims.catalog.api.AnalysisId,
 * java.time.Clock)}.
 */
public record NewReferenceInterval(
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
