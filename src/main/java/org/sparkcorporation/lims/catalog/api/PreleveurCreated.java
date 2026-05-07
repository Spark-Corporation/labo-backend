package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record PreleveurCreated(PreleveurId id, String code, Instant createdAt) {

    public PreleveurCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
