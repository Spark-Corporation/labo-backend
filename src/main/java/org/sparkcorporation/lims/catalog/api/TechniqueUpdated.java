package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record TechniqueUpdated(TechniqueId id, Instant changedAt) {

    public TechniqueUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
