package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record InsuranceUpdated(InsuranceId id, Instant changedAt) {

    public InsuranceUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
