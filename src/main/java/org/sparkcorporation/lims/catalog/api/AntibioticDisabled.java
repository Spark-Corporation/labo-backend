package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record AntibioticDisabled(AntibioticId id, Instant disabledAt) {

    public AntibioticDisabled {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(disabledAt, "disabledAt");
    }
}
