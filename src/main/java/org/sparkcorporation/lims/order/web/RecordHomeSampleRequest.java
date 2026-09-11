package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST {@code /api/v1/lab-files/{id}/samples:home} payload — records a home-collected sample.
 *
 * <p>{@code preleveurId} is required (Bean Validation {@link NotBlank} + a domain backstop in
 * {@link org.sparkcorporation.lims.order.domain.HomeSample#create}).
 */
@Schema(description = "Request to record a sample collected at the patient's home by a Préleveur.")
public record RecordHomeSampleRequest(
        @Schema(description = "Identifier of the catalog analysis this sample is for.")
                @NotBlank
                @Size(max = 26)
                String analysisId,
        @Schema(description = "Identifier of the sample recipient (specimen container/type) used.")
                @NotBlank
                @Size(max = 26)
                String recipientId,
        @Schema(description = "Timestamp at which the sample was actually collected.") @NotNull
                Instant collectedAt,
        @Schema(description = "Patient's home address where the collection took place.")
                @NotBlank
                @Size(max = 255)
                String addressLine,
        @Schema(
                        description =
                                "Distance in kilometers traveled by the Préleveur for this"
                                        + " collection.")
                @PositiveOrZero
                int distanceKm,
        @Schema(description = "Start of the scheduled collection time window.") @NotNull
                Instant windowStart,
        @Schema(description = "End of the scheduled collection time window.") @NotNull
                Instant windowEnd,
        @Schema(
                        description =
                                "Identifier of the Préleveur (home-collection agent) who performed"
                                        + " the collection. Required.")
                @NotBlank
                @Size(max = 26)
                String preleveurId) {}
