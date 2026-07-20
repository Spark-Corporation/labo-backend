package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Request body for {@code PUT /api/v1/catalog/analyses/{id}/intervals}.
 *
 * <p>Full-replace semantics: existing intervals soft-deleted, new ones persisted. Mode-specific
 * invariants validated by {@code ReferenceInterval.create}.
 */
public record PutIntervalsRequest(@NotNull @Valid List<@Valid IntervalPayload> items) {

    public record IntervalPayload(
            @NotNull PatientCategory category,
            @Nullable Sex sex,
            @Nullable Integer ageMin,
            @Nullable Integer ageMax,
            @Nullable AgeUnit ageUnit,
            @NotNull ReferenceMode mode,
            @Nullable BigDecimal unit1Min,
            @Nullable BigDecimal unit1Max,
            @Nullable BigDecimal unit2Min,
            @Nullable BigDecimal unit2Max,
            @Nullable String commentText) {}
}
