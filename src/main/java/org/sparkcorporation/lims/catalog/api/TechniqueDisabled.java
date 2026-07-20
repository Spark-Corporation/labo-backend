package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record TechniqueDisabled(TechniqueId id, Instant disabledAt) {

    public TechniqueDisabled {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(disabledAt, "disabledAt");
    }
}
