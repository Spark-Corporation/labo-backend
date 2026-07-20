package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.catalog.domain.Prescriber}. */
public record PrescriberId(String value) {

    public PrescriberId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static PrescriberId generate() {
        return new PrescriberId(Ulid.generate());
    }

    public static PrescriberId of(String value) {
        return new PrescriberId(value);
    }
}
