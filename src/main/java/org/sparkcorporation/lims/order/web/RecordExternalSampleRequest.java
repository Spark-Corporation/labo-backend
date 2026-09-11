package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * POST {@code /api/v1/lab-files/{id}/samples:external} payload — records an externally-collected
 * sample (received from a partner site).
 *
 * <p>{@code collectedAt} is nullable per CDC §6.9 (provider may not record collection time); {@code
 * receivedAt} is required.
 */
@Schema(
        description =
                "Request to record a sample that was collected externally (at a partner site) and"
                        + " received at the lab.")
public record RecordExternalSampleRequest(
        @Schema(description = "Identifier of the catalog analysis this sample is for.")
                @NotBlank
                @Size(max = 26)
                String analysisId,
        @Schema(description = "Identifier of the sample recipient (specimen container/type) used.")
                @NotBlank
                @Size(max = 26)
                String recipientId,
        @Schema(
                        description =
                                "Timestamp at which the external provider collected the sample, if"
                                        + " reported; not all providers record this.")
                @Nullable Instant collectedAt,
        @Schema(description = "Timestamp at which the lab received the sample. Required.") @NotNull
                Instant receivedAt,
        @Schema(description = "Identifier of the staff user who received the sample at the lab.")
                @NotBlank
                @Size(max = 26)
                String receivedByUserId,
        @Schema(
                        description =
                                "Free-text origin of the sample (the sending partner site or"
                                        + " provider).")
                @NotBlank
                @Size(max = 120)
                String origin) {}
