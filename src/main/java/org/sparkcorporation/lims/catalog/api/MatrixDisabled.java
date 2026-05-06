package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record MatrixDisabled(MatrixId id, Instant disabledAt) {

    public MatrixDisabled {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(disabledAt, "disabledAt");
    }
}
