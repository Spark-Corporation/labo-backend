package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Search response wrapper for {@link AnalysisResponse}. */
@Schema(description = "Result of an analysis search.")
public record AnalysisSearchResponse(
        @Schema(description = "Analyses matching the search criteria.")
                List<AnalysisResponse> items) {}
