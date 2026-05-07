package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.catalog.domain.Specialty}. */
public record SpecialtyId(String value) {

    public SpecialtyId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static SpecialtyId generate() {
        return new SpecialtyId(Ulid.generate());
    }

    public static SpecialtyId of(String value) {
        return new SpecialtyId(value);
    }
}
