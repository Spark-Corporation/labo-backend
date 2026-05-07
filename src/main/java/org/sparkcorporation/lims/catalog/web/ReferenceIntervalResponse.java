package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/** Response DTO for a {@code ReferenceInterval}. */
public record ReferenceIntervalResponse(
        String id,
        String analysisId,
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
        @Nullable String commentText,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
