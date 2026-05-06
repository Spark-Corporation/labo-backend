package org.sparkcorporation.lims.iam.api;

import java.time.Instant;
import java.util.Objects;

public record RoleAssigned(UserId id, Role role, Instant assignedAt) {

    public RoleAssigned {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
