package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record DisciplineDisabled(DisciplineId id, Instant disabledAt) {

    public DisciplineDisabled {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(disabledAt, "disabledAt");
    }
}
