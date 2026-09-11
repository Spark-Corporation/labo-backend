package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Web-tier mirror of {@link
 * org.sparkcorporation.lims.order.api.LabFileSummary.AnalysisRequestSummary}.
 */
@Schema(description = "One catalog analysis requested on a lab file.")
public record AnalysisRequestResponse(
        @Schema(description = "Unique identifier of this analysis request line on the lab file.")
                String id,
        @Schema(description = "Identifier of the catalog analysis that was requested.")
                String analysisId,
        @Schema(
                        description =
                                "Catalog code of the requested analysis, as resolved at request"
                                        + " time.")
                String analysisCode,
        @Schema(description = "Timestamp at which the analysis was added to the lab file.")
                Instant requestedAt) {}
