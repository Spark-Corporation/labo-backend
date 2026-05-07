package org.sparkcorporation.lims.catalog.web;

import org.jspecify.annotations.Nullable;

/**
 * Embedded {@code AnalyticalDelay} value object request shape per spec §5 — sub-record on
 * create/update analysis requests. All fields nullable.
 */
public record AnalyticalDelayRequest(
        @Nullable Integer days, @Nullable Integer hours, @Nullable Integer anteriorityCount) {}
