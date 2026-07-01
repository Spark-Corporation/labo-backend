package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Immutable projection of an {@code Analysis} revision's own configuration fields (scalars +
 * embedded {@code Units}/{@code AnalyticalDelay}), used to compute the changed-field list between
 * two consecutive audit revisions (SP12 config history). Components, techniques and reference
 * intervals are audited as independent entities and surfaced as their own history timelines, not as
 * fields here.
 */
public record AnalysisSnapshot(
        String name,
        BigDecimal indiciaryValueB,
        int labelCount,
        String resultType,
        String kind,
        @Nullable String interpretationText,
        @Nullable String formulaExpression,
        @Nullable String defaultPortoir,
        @Nullable Integer defaultConservationDays,
        @Nullable String matrixId,
        @Nullable String recipientId,
        @Nullable String unit1,
        @Nullable Integer unit1Decimals,
        @Nullable String unit2,
        @Nullable Integer unit2Decimals,
        @Nullable BigDecimal conversionFactor,
        @Nullable Integer delayDays,
        @Nullable Integer delayHours,
        @Nullable Integer anteriorityCount) {}
