package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * POST {@code /api/v1/lab-files/{id}/samples:external} payload — records an externally-collected
 * sample (received from a partner site).
 *
 * <p>{@code collectedAt} is nullable per CDC §6.9 (provider may not record collection time); {@code
 * receivedAt} is required.
 */
public record RecordExternalSampleRequest(
        @NotBlank @Size(max = 26) String analysisId,
        @NotBlank @Size(max = 26) String recipientId,
        @Nullable Instant collectedAt,
        @NotNull Instant receivedAt,
        @NotBlank @Size(max = 26) String receivedByUserId,
        @NotBlank @Size(max = 120) String origin) {}
