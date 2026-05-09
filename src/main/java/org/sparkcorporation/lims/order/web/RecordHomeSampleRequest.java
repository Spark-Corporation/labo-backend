package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * POST {@code /api/v1/lab-files/{id}/samples:home} payload — records a home-collected sample.
 *
 * <p>{@code preleveurId} is required (Bean Validation {@link NotBlank} + a domain backstop in
 * {@link org.sparkcorporation.lims.order.domain.HomeSample#create}).
 */
public record RecordHomeSampleRequest(
        @NotBlank @Size(max = 26) String analysisId,
        @NotBlank @Size(max = 26) String recipientId,
        @NotNull Instant collectedAt,
        @NotBlank @Size(max = 255) String addressLine,
        @PositiveOrZero int distanceKm,
        @NotNull Instant windowStart,
        @NotNull Instant windowEnd,
        @NotBlank @Size(max = 26) String preleveurId) {}
