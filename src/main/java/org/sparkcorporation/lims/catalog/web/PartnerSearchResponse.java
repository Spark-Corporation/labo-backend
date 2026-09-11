package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated result of a partner search.")
public record PartnerSearchResponse(
        @Schema(description = "Partners matching the search criteria.") List<PartnerResponse> items,
        @Schema(description = "Total number of partners matching the search.")
                long totalElements) {}
