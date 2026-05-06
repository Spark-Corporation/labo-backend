package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record DisciplineUpdated(DisciplineId id, Instant changedAt) {

    public DisciplineUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
