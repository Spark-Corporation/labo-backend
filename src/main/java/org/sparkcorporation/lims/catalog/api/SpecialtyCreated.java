package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record SpecialtyCreated(SpecialtyId id, String code, Instant createdAt) {

    public SpecialtyCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
