package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record PartnerCreated(PartnerId id, String code, Instant createdAt) {

    public PartnerCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
