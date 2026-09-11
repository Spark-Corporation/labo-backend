package org.sparkcorporation.lims.order.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST {@code /api/v1/quotes/{id}/lines} payload — appends analysis codes onto a DRAFT quote.
 *
 * <p>Each code is resolved against {@code AnalysisApi.findByCode} and dedup-checked (within the
 * request) before persistence. Mirrors {@link CreateQuoteRequest#analyses()} shape.
 */
@Schema(
        description =
                "Request to append one or more catalog analyses as new lines on a DRAFT quote.")
public record AddQuoteLinesRequest(
        @Schema(
                        description =
                                "Catalog analysis codes to append as new quote lines; only allowed"
                                        + " while the quote is still DRAFT.")
                @NotEmpty
                @Size(max = 50)
                List<@NotBlank @Size(max = 32) String> analyses) {}
