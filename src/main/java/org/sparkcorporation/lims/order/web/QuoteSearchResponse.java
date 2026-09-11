package org.sparkcorporation.lims.order.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Paginated GET {@code /api/v1/quotes} response envelope. Mirrors {@link LabFileSearchResponse}.
 */
@Schema(description = "One page of quotes matching the search filters.")
public record QuoteSearchResponse(
        @Schema(description = "Quotes on this page.") List<QuoteResponse> content,
        @Schema(description = "Zero-based page number returned.") int page,
        @Schema(description = "Number of items requested per page.") int size,
        @Schema(description = "Total number of quotes matching the filters, across all pages.")
                long totalElements,
        @Schema(description = "Total number of pages available.") int totalPages) {

    public QuoteSearchResponse {
        content = List.copyOf(content);
    }
}
