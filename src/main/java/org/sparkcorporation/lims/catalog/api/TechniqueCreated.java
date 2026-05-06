package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record TechniqueCreated(TechniqueId id, String code, Instant createdAt) {

    public TechniqueCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
