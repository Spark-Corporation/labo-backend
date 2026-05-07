package org.sparkcorporation.lims.catalog.web;

import java.util.List;

/** Search response wrapper for {@link AnalysisResponse}. */
public record AnalysisSearchResponse(List<AnalysisResponse> items) {}
