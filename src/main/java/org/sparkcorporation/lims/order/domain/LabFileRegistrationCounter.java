package org.sparkcorporation.lims.order.domain;

import java.time.LocalDate;

/**
 * Per-day file-level sequence counter for LabFile registration numbers. Implementation backs the
 * UPSERT-with-RETURNING pattern; chunk 2 ships the JPA implementation.
 */
public interface LabFileRegistrationCounter {

    /**
     * Atomically increments the counter for the given {@code day} and returns the post-increment
     * value.
     */
    int incrementAndGet(LocalDate day);
}
