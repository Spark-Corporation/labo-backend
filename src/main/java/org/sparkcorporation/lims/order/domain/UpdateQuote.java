package org.sparkcorporation.lims.order.domain;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * PATCH-style update command for a DRAFT {@link Quote}. Each field is nullable; a {@code null}
 * sentinel means "no change". Whitelist mirrors spec §5.4: only {@code prescriberId} / {@code
 * expiresAt} are mutable post-creation. {@code quoteNumber} / {@code patientId} / {@code status}
 * are rejected at the controller layer.
 */
public record UpdateQuote(@Nullable String prescriberId, @Nullable Instant expiresAt) {}
