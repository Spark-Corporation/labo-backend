package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/** All fields nullable; null = no change. {@code analysisId} is NOT modifiable. */
public record UpdateReferenceInterval(
        @Nullable PatientCategory category,
        @Nullable Sex sex,
        @Nullable Integer ageMin,
        @Nullable Integer ageMax,
        @Nullable AgeUnit ageUnit,
        @Nullable ReferenceMode mode,
        @Nullable BigDecimal unit1Min,
        @Nullable BigDecimal unit1Max,
        @Nullable BigDecimal unit2Min,
        @Nullable BigDecimal unit2Max,
        @Nullable String commentText) {}
