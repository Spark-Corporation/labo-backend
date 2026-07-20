package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record SpecialtyDisabled(SpecialtyId id, Instant disabledAt) {

    public SpecialtyDisabled {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(disabledAt, "disabledAt");
    }
}
