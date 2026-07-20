package org.sparkcorporation.lims.order.api;

import java.time.Instant;

/** Domain event — fired after an analysis is removed from a LabFile. */
public record AnalysisRequestRemoved(
        LabFileId labFileId,
        AnalysisRequestId analysisRequestId,
        String analysisCode,
        Instant occurredAt) {}
