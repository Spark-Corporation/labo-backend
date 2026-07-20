package org.sparkcorporation.lims.order.web;

import java.util.List;

/**
 * Paginated GET {@code /api/v1/quotes} response envelope. Mirrors {@link LabFileSearchResponse}.
 */
public record QuoteSearchResponse(
        List<QuoteResponse> content, int page, int size, long totalElements, int totalPages) {

    public QuoteSearchResponse {
        content = List.copyOf(content);
    }
}
