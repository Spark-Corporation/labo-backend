package org.sparkcorporation.lims.billing.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Wrapper for {@code GET /api/v1/cash-registers}. */
@Schema(description = "One page of cash register search results.")
public record CashRegisterSearchResponse(
        @Schema(description = "Cash registers on this page.") List<CashRegisterResponse> items,
        @Schema(description = "Total number of registers matching the search, across all pages.")
                long totalElements) {}
