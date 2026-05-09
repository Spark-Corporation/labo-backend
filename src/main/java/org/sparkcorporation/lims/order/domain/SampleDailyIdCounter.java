package org.sparkcorporation.lims.order.domain;

import java.time.LocalDate;

/**
 * Per-(day, disciplinePrefix) sequence counter for sample daily-IDs. Implementation backs the
 * UPSERT-with-RETURNING pattern; chunk 2 ships the JPA implementation.
 */
public interface SampleDailyIdCounter {

    /**
     * Atomically increments the counter for the given {@code (day, disciplinePrefix)} pair and
     * returns the post-increment value (i.e. {@code 1} on first call, {@code 2} on second, etc.).
     *
     * <p>Caller is responsible for validating {@code disciplinePrefix} is in the regulatory range
     * {@code 1..5} per CDC §6.6 — the JPA implementation binds the int as a {@code CHAR(1)} column
     * value, so out-of-range values may fail less obviously at the DB layer. {@code
     * DefaultSampleDailyIdGenerator} owns this validation in production callers.
     */
    int incrementAndGet(LocalDate day, int disciplinePrefix);
}
