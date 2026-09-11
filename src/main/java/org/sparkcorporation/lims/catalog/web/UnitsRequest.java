package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Embedded {@code Units} value object request shape per spec §5 — sub-record on create/update
 * analysis requests. All fields nullable; decimals must be in {@code [0, 5]}.
 */
@Schema(
        description =
                "Result unit configuration; supports an optional secondary unit derived by a"
                        + " conversion factor.")
public record UnitsRequest(
        @Schema(description = "Primary result unit label (e.g. 'g/L').") @Size(max = 20)
                @Nullable String unit1,
        @Schema(description = "Number of decimal places to display for unit1, in [0,5].")
                @Min(0)
                @Max(5)
                @Nullable Integer unit1Decimals,
        @Schema(
                        description =
                                "Secondary result unit label, if the result is also expressed in"
                                        + " another unit.")
                @Size(max = 20)
                @Nullable String unit2,
        @Schema(description = "Number of decimal places to display for unit2, in [0,5].")
                @Min(0)
                @Max(5)
                @Nullable Integer unit2Decimals,
        @Schema(description = "Multiplier used to convert a value expressed in unit1 to unit2.")
                @Nullable BigDecimal conversionFactor) {}
