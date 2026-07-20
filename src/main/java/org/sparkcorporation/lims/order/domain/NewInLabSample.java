package org.sparkcorporation.lims.order.domain;

import java.time.Instant;

/** Command record for creating an {@link InLabSample}. */
public record NewInLabSample(String analysisId, String recipientId, Instant collectedAt) {}
