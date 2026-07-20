package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.catalog.domain.Technique}. */
public record TechniqueId(String value) {

    public TechniqueId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static TechniqueId next() {
        return new TechniqueId(Ulid.generate());
    }
}
