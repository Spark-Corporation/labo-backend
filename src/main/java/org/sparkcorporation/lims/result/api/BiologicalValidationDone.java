package org.sparkcorporation.lims.result.api;

import java.time.Instant;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Domain event — fired after biologist's {@code :validate-biological} call (TECHNICAL_VALIDATED →
 * BIOLOGICAL_VALIDATED). Triggers downstream: billing 1.6 invoice finalization, delivery 1.8
 * publish, stats 1.9 projection.
 */
public record BiologicalValidationDone(
        ResultId resultId,
        LabFileId labFileId,
        AnalysisRequestId analysisRequestId,
        String biologicalValidatedBy,
        Instant occurredAt) {}
