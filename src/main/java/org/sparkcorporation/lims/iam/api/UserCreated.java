package org.sparkcorporation.lims.iam.api;

import java.time.Instant;
import java.util.Objects;

public record UserCreated(UserId id, String email, Instant createdAt) {

    public UserCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
