package org.sparkcorporation.lims.billing.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Page wrapper for {@code GET /api/v1/invoices}. */
@Schema(description = "One page of invoice search results.")
public record InvoiceSearchResponse(
        @Schema(description = "Invoices on this page.") List<InvoiceResponse> items,
        @Schema(description = "Total number of invoices matching the search, across all pages.")
                long total) {}
