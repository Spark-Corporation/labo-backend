package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record AnalysisDuplicated(
        AnalysisId sourceId, AnalysisId newId, String newCode, Instant createdAt) {

    public AnalysisDuplicated {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(newId, "newId");
        Objects.requireNonNull(newCode, "newCode");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
