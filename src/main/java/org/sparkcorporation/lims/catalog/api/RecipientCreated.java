package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record RecipientCreated(RecipientId id, String code, Instant createdAt) {

    public RecipientCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
