package org.sparkcorporation.lims.cashbox.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Page of expenses matching the requested filters")
public record ExpenseSearchResponse(
        @Schema(description = "Matching expenses on this page") List<ExpenseResponse> items,
        @Schema(description = "Total number of matching expenses, across all pages")
                long totalElements) {}
