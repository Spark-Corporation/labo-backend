package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record RecipientUpdated(RecipientId id, Instant changedAt) {

    public RecipientUpdated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
