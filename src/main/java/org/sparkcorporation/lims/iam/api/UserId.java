package org.sparkcorporation.lims.iam.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID identifier for a {@link org.sparkcorporation.lims.iam.domain.User}. */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static UserId next() {
        return new UserId(Ulid.generate());
    }
}
