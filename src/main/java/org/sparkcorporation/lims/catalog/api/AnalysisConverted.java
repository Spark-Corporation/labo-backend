package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record AnalysisConverted(
        AnalysisId id, AnalysisKind from, AnalysisKind to, Instant changedAt) {

    public AnalysisConverted {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
