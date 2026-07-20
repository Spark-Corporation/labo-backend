package org.sparkcorporation.lims.order.api;

import java.time.Instant;

/** Domain event emitted when a {@code Quote} is created (DRAFT). */
public record QuoteCreated(
        QuoteId quoteId, String patientId, QuoteNumber quoteNumber, Instant occurredAt) {}
