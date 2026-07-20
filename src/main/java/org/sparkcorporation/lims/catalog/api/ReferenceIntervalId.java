package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.catalog.domain.ReferenceInterval}. */
public record ReferenceIntervalId(String value) {

    public ReferenceIntervalId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static ReferenceIntervalId generate() {
        return new ReferenceIntervalId(Ulid.generate());
    }

    public static ReferenceIntervalId of(String value) {
        return new ReferenceIntervalId(value);
    }
}
