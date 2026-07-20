package org.sparkcorporation.lims.order.domain;

import java.time.Instant;

/** Command record for creating a {@link HomeSample}. */
public record NewHomeSample(
        String analysisId,
        String recipientId,
        Instant collectedAt,
        String addressLine,
        int distanceKm,
        Instant windowStart,
        Instant windowEnd,
        String homePreleveurId) {}
