package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST {@code /api/v1/lab-files/{id}/samples:in-lab} payload — records an in-lab counter sample.
 */
public record RecordInLabSampleRequest(
        @NotBlank @Size(max = 26) String analysisId,
        @NotBlank @Size(max = 26) String recipientId,
        @NotNull Instant collectedAt) {}
