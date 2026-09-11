package org.sparkcorporation.lims.result.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST body for {@code POST /api/v1/results/{id}/antibiogram}. {@code measuredDiameter} is
 * string-encoded {@code BigDecimal} to preserve precision (mirrors the {@code REAL} value
 * convention in {@link PatchLineRequest.ValueUnion.RealVariant}). The {@code @Pattern} regex
 * rejects malformed numeric input at the controller boundary so a {@code NumberFormatException}
 * from {@code new BigDecimal(...)} cannot escape the service into the generic 500 advice.
 */
@Schema(
        description =
                "Antibiogram reading to record: one antibiotic + its measured inhibition diameter")
public record AddAntibiogramReadingRequest(
        @Schema(description = "ULID of the catalog antibiotic that was tested") @NotBlank
                String antibioticId,
        @Schema(
                        description =
                                "Measured inhibition zone diameter in millimeters, as a decimal"
                                        + " string (e.g. \"18.5\") to preserve precision")
                @NotBlank
                @Pattern(regexp = "^-?\\d+(\\.\\d+)?$", message = "must be a valid decimal number")
                String measuredDiameter) {}
