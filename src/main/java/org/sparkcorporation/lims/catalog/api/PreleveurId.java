package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.catalog.domain.Preleveur}. */
public record PreleveurId(String value) {

    public PreleveurId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static PreleveurId generate() {
        return new PreleveurId(Ulid.generate());
    }

    public static PreleveurId of(String value) {
        return new PreleveurId(value);
    }
}
