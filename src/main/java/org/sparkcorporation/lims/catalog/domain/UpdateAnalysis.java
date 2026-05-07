package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.MatrixId;
import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.api.ResultType;

/**
 * All fields nullable; null = no change. {@code code} and {@code kind} are NOT modifiable via
 * update — use the convert endpoints to change kind. {@code disciplineId} is also immutable here
 * (rebinding an analysis to a new discipline reshapes too much downstream history).
 */
public record UpdateAnalysis(
        @Nullable String name,
        @Nullable BigDecimal indiciaryValueB,
        @Nullable Integer labelCount,
        @Nullable MatrixId matrixId,
        @Nullable RecipientId recipientId,
        @Nullable ResultType resultType,
        @Nullable String interpretationText,
        @Nullable String defaultPortoir,
        @Nullable Integer defaultConservationDays,
        @Nullable Units units,
        @Nullable AnalyticalDelay delay) {}
