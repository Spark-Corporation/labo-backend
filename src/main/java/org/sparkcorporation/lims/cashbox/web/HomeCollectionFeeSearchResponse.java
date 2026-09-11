package org.sparkcorporation.lims.cashbox.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Page of home-collection fees matching the requested filters")
public record HomeCollectionFeeSearchResponse(
        @Schema(description = "Matching home-collection fees on this page")
                List<HomeCollectionFeeResponse> items,
        @Schema(description = "Total number of matching fees, across all pages")
                long totalElements) {}
