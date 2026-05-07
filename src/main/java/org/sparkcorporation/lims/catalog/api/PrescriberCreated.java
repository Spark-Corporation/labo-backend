package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record PrescriberCreated(PrescriberId id, String code, Instant createdAt) {

    public PrescriberCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
