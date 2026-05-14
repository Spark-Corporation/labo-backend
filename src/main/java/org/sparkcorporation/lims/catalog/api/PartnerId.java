package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.catalog.domain.Partner}. */
public record PartnerId(String value) {

    public PartnerId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static PartnerId generate() {
        return new PartnerId(Ulid.generate());
    }

    public static PartnerId of(String value) {
        return new PartnerId(value);
    }
}
