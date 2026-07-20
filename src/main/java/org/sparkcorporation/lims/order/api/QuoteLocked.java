package org.sparkcorporation.lims.order.api;

import java.math.BigDecimal;
import java.time.Instant;

/** Domain event emitted when a {@code Quote} transitions DRAFT → LOCKED with a frozen pricing. */
public record QuoteLocked(QuoteId quoteId, BigDecimal totalAmount, Instant occurredAt) {}
