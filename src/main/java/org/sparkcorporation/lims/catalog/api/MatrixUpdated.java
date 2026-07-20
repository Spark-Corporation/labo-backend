package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record MatrixUpdated(MatrixId id, Instant changedAt) {

    public MatrixUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
