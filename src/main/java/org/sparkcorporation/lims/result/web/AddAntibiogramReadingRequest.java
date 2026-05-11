package org.sparkcorporation.lims.result.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * POST body for {@code POST /api/v1/results/{id}/antibiogram}. {@code measuredDiameter} is
 * string-encoded {@code BigDecimal} to preserve precision (mirrors the {@code REAL} value
 * convention in {@link PatchLineRequest.ValueUnion.RealVariant}). The {@code @Pattern} regex
 * rejects malformed numeric input at the controller boundary so a {@code NumberFormatException}
 * from {@code new BigDecimal(...)} cannot escape the service into the generic 500 advice.
 */
public record AddAntibiogramReadingRequest(
        @NotBlank String antibioticId,
        @NotBlank
                @Pattern(regexp = "^-?\\d+(\\.\\d+)?$", message = "must be a valid decimal number")
                String measuredDiameter) {}
