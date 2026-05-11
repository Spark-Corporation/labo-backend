package org.sparkcorporation.lims.result.api;

import java.time.Instant;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Domain event — fired after a {@code Result} is eagerly created by the AnalysisRequest event
 * handler.
 */
public record ResultCreated(
        ResultId resultId,
        LabFileId labFileId,
        AnalysisRequestId analysisRequestId,
        AnalysisId analysisId,
        Instant occurredAt) {}
