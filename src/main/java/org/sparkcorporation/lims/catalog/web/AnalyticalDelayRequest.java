package org.sparkcorporation.lims.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Embedded {@code AnalyticalDelay} value object request shape per spec §5 — sub-record on
 * create/update analysis requests. All fields nullable.
 */
@Schema(description = "Expected turnaround delay for the analysis result.")
public record AnalyticalDelayRequest(
        @Schema(description = "Number of whole days of turnaround delay.") @Nullable Integer days,
        @Schema(description = "Additional hours of turnaround delay, on top of 'days'.")
                @Nullable Integer hours,
        @Schema(
                        description =
                                "Number of prior/anteriority results required before this delay"
                                        + " applies.")
                @Nullable Integer anteriorityCount) {}
