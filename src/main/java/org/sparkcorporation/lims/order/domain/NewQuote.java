package org.sparkcorporation.lims.order.domain;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Command for creating a {@link Quote}. {@code analysisCodes} captured for cross-module validation
 * in {@code QuoteService.create} (resolution + dedup happens before the factory call). Defensive
 * copy at construction blocks aliasing.
 */
public record NewQuote(
        String patientId,
        @Nullable String prescriberId,
        @Nullable Instant expiresAt,
        List<String> analysisCodes) {

    public NewQuote {
        analysisCodes = List.copyOf(analysisCodes);
    }
}
