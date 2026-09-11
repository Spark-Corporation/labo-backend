package org.sparkcorporation.lims.order.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * POST {@code /api/v1/quotes} payload — opens a new quote at status {@code DRAFT}.
 *
 * <p>{@code patientId} required; {@code prescriberId} optional. {@code analyses} is a non-empty
 * list of analysis codes (catalog reference) that the service resolves via {@code
 * AnalysisApi.findByCode} and dedup-checks before bumping the quote-number counter.
 */
@Schema(description = "Request to open a new price quote (devis) for a patient, at status DRAFT.")
public record CreateQuoteRequest(
        @Schema(description = "Identifier of the patient this quote is drawn up for. Required.")
                @NotBlank
                @Size(max = 26)
                String patientId,
        @Schema(description = "Identifier of the prescribing doctor, if any.") @Size(max = 26)
                @Nullable String prescriberId,
        @Schema(
                        description =
                                "Catalog analysis codes to quote; resolved and dedup-checked before"
                                        + " persistence. Must not be empty.")
                @NotEmpty
                @Size(max = 50)
                List<@NotBlank @Size(max = 32) String> analyses,
        @Schema(description = "Date after which this quote is no longer valid, if any.")
                @Nullable Instant expiresAt) {}
