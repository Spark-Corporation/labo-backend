package org.sparkcorporation.lims.iam.api;

import java.time.Instant;
import java.util.Objects;

public record UserDisabled(UserId id, Instant disabledAt) {

    public UserDisabled {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(disabledAt, "disabledAt");
    }
}
