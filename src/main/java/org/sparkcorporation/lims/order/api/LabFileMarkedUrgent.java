package org.sparkcorporation.lims.order.api;

import java.time.Instant;

/** Domain event — idempotent flip of {@code urgent} flag from false → true (spec §5.5). */
public record LabFileMarkedUrgent(LabFileId labFileId, Instant occurredAt) {}
