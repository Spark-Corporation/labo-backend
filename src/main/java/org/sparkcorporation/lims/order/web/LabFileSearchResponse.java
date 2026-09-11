package org.sparkcorporation.lims.order.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Paginated GET {@code /api/v1/lab-files} response envelope. */
@Schema(description = "One page of lab files matching the search filters.")
public record LabFileSearchResponse(
        @Schema(description = "Lab files on this page.") List<LabFileResponse> content,
        @Schema(description = "Zero-based page number returned.") int page,
        @Schema(description = "Number of items requested per page.") int size,
        @Schema(description = "Total number of lab files matching the filters, across all pages.")
                long totalElements,
        @Schema(description = "Total number of pages available.") int totalPages) {

    public LabFileSearchResponse {
        content = List.copyOf(content);
    }
}
