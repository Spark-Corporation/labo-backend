package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record AnalysisCreated(AnalysisId id, String code, AnalysisKind kind, Instant createdAt) {

    public AnalysisCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
