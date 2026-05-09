package org.sparkcorporation.lims.order.api;

import java.time.Instant;

/** Domain event emitted when a {@code Quote} transitions LOCKED → CONVERTED into a LabFile. */
public record QuoteConverted(QuoteId quoteId, String labFileId, Instant occurredAt) {}
