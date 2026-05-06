package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record MatrixCreated(MatrixId id, String code, Instant createdAt) {

    public MatrixCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
