package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ResultType;

/**
 * PATCH semantics: all fields nullable; null = no change. {@code code} and {@code kind} are
 * intentionally absent — both are immutable per spec §5.5; use the {@code :convert-to-grouped} /
 * {@code :convert-to-simple} endpoints to change kind. The controller binds the body as a {@code
 * Map} first to detect and reject those fields explicitly with 422, then delegates here.
 */
public record UpdateAnalysisRequest(
        @Size(max = 160) @Nullable String name,
        @DecimalMin("0.0") @Nullable BigDecimal indiciaryValueB,
        @Min(0) @Nullable Integer labelCount,
        @Size(max = 26) @Nullable String matrixId,
        @Size(max = 26) @Nullable String recipientId,
        @Nullable ResultType resultType,
        @Nullable String interpretationText,
        @Size(max = 20) @Nullable String defaultPortoir,
        @Nullable Integer defaultConservationDays,
        @Valid @Nullable UnitsRequest units,
        @Valid @Nullable AnalyticalDelayRequest delay) {}
