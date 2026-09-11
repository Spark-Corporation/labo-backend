package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated result of a sample recipient search.")
public record RecipientSearchResponse(
        @Schema(description = "Recipients on this page.") List<RecipientResponse> content,
        @Schema(description = "Zero-based index of this page.") int page,
        @Schema(description = "Requested page size.") int size,
        @Schema(description = "Total number of recipients matching the search.") long totalElements,
        @Schema(description = "Total number of pages.") int totalPages) {}
