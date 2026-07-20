package org.sparkcorporation.lims.iam.api;

import java.time.Instant;
import java.util.Objects;

public record UserPasswordChanged(UserId id, Instant changedAt) {

    public UserPasswordChanged {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(changedAt, "changedAt");
    }
}
