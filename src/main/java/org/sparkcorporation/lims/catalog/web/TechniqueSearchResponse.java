package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated result of an analytical technique search.")
public record TechniqueSearchResponse(
        @Schema(description = "Techniques on this page.") List<TechniqueResponse> content,
        @Schema(description = "Zero-based index of this page.") int page,
        @Schema(description = "Requested page size.") int size,
        @Schema(description = "Total number of techniques matching the search.") long totalElements,
        @Schema(description = "Total number of pages.") int totalPages) {}
