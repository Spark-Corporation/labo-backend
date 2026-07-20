package org.sparkcorporation.lims.order.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * POST {@code /api/v1/quotes/{id}/lines} payload — appends analysis codes onto a DRAFT quote.
 *
 * <p>Each code is resolved against {@code AnalysisApi.findByCode} and dedup-checked (within the
 * request) before persistence. Mirrors {@link CreateQuoteRequest#analyses()} shape.
 */
public record AddQuoteLinesRequest(
        @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 32) String> analyses) {}
