package org.sparkcorporation.lims.order.domain;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/** Command record for creating an {@link ExternalReceivedSample}. */
public record NewExternalReceivedSample(
        String analysisId,
        String recipientId,
        @Nullable Instant collectedAt,
        Instant receivedAt,
        String receivedByUserId,
        String origin) {}
