package org.sparkcorporation.lims.order.domain;

import java.time.LocalDate;

/**
 * Per-day sequence counter for quote numbers. Implementation backs UPSERT-with-RETURNING (mirrors
 * {@link LabFileRegistrationCounter}); chunk 1 ships the JPA implementation.
 */
public interface QuoteNumberCounter {

    /**
     * Atomically increments the counter for {@code day} and returns the post-increment value
     * ({@code 1} on first call, {@code 2} on second, etc.).
     */
    int incrementAndGet(LocalDate day);
}
