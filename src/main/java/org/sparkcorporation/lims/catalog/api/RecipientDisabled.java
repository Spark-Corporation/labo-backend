package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;

public record RecipientDisabled(RecipientId id, Instant disabledAt) {

    public RecipientDisabled {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(disabledAt, "disabledAt");
    }
}
