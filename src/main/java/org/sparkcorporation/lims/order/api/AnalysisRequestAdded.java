package org.sparkcorporation.lims.order.api;

import java.time.Instant;

/** Domain event — fired after an analysis is added to a LabFile. */
public record AnalysisRequestAdded(
        LabFileId labFileId,
        AnalysisRequestId analysisRequestId,
        String analysisCode,
        Instant occurredAt) {}
