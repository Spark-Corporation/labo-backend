package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated result of an antibiotic search.")
public record AntibioticSearchResponse(
        @Schema(description = "Antibiotics on this page.") List<AntibioticResponse> content,
        @Schema(description = "Zero-based index of this page.") int page,
        @Schema(description = "Requested page size.") int size,
        @Schema(description = "Total number of antibiotics matching the search.")
                long totalElements,
        @Schema(description = "Total number of pages.") int totalPages) {}
