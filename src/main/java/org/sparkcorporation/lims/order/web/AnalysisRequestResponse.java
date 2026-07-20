package org.sparkcorporation.lims.order.web;

import java.time.Instant;

/**
 * Web-tier mirror of {@link
 * org.sparkcorporation.lims.order.api.LabFileSummary.AnalysisRequestSummary}.
 */
public record AnalysisRequestResponse(
        String id, String analysisId, String analysisCode, Instant requestedAt) {}
