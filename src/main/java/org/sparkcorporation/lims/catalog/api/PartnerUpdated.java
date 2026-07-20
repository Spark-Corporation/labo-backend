package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record PartnerUpdated(PartnerId id, Instant changedAt) {

    public PartnerUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
