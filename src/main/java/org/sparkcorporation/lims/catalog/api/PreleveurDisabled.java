package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record PreleveurDisabled(PreleveurId id, Instant disabledAt) {

    public PreleveurDisabled {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(disabledAt, "disabledAt");
    }
}
