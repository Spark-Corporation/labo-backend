package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record InsuranceCreated(InsuranceId id, String code, Instant createdAt) {

    public InsuranceCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
