package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record SpecialtyUpdated(SpecialtyId id, Instant changedAt) {

    public SpecialtyUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
