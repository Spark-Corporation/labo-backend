package org.sparkcorporation.lims.order.web;

import org.sparkcorporation.lims.order.api.LabFileSummary;

/**
 * Hand-written mapper translating order analysis-request summaries → web responses. Static-method
 * style mirrors the catalog precedent.
 */
public final class AnalysisRequestMapper {

    private AnalysisRequestMapper() {}

    public static AnalysisRequestResponse toResponse(LabFileSummary.AnalysisRequestSummary s) {
        return new AnalysisRequestResponse(
                s.id().value(), s.analysisId(), s.analysisCode(), s.requestedAt());
    }
}
