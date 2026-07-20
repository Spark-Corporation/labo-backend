package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Embedded {@code Units} value object request shape per spec §5 — sub-record on create/update
 * analysis requests. All fields nullable; decimals must be in {@code [0, 5]}.
 */
public record UnitsRequest(
        @Size(max = 20) @Nullable String unit1,
        @Min(0) @Max(5) @Nullable Integer unit1Decimals,
        @Size(max = 20) @Nullable String unit2,
        @Min(0) @Max(5) @Nullable Integer unit2Decimals,
        @Nullable BigDecimal conversionFactor) {}
