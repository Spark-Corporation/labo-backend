package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record AnalysisUpdated(AnalysisId id, Instant changedAt) {

    public AnalysisUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
