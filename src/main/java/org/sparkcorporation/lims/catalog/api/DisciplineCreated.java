package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record DisciplineCreated(DisciplineId id, String code, Instant createdAt) {

    public DisciplineCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
