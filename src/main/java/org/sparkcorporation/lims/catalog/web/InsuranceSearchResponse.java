package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated result of an insurance search.")
public record InsuranceSearchResponse(
        @Schema(description = "Insurances matching the search criteria.")
                List<InsuranceResponse> items,
        @Schema(description = "Total number of insurances matching the search.")
                long totalElements) {}
