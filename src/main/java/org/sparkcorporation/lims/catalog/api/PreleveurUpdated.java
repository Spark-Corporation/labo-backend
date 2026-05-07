package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record PreleveurUpdated(PreleveurId id, Instant changedAt) {

    public PreleveurUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
