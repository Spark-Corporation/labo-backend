package org.sparkcorporation.lims.order.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * POST {@code /api/v1/quotes} payload — opens a new quote at status {@code DRAFT}.
 *
 * <p>{@code patientId} required; {@code prescriberId} optional. {@code analyses} is a non-empty
 * list of analysis codes (catalog reference) that the service resolves via {@code
 * AnalysisApi.findByCode} and dedup-checks before bumping the quote-number counter.
 */
public record CreateQuoteRequest(
        @NotBlank @Size(max = 26) String patientId,
        @Size(max = 26) @Nullable String prescriberId,
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 32) String> analyses,
        @Nullable Instant expiresAt) {}
