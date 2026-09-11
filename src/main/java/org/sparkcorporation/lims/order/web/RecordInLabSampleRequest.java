package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST {@code /api/v1/lab-files/{id}/samples:in-lab} payload — records an in-lab counter sample.
 */
@Schema(description = "Request to record a sample collected in-lab, at the reception counter.")
public record RecordInLabSampleRequest(
        @Schema(description = "Identifier of the catalog analysis this sample is for.")
                @NotBlank
                @Size(max = 26)
                String analysisId,
        @Schema(description = "Identifier of the sample recipient (specimen container/type) used.")
                @NotBlank
                @Size(max = 26)
                String recipientId,
        @Schema(description = "Timestamp at which the sample was collected.") @NotNull
                Instant collectedAt) {}
