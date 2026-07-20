package org.sparkcorporation.lims.result.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Typed ULID wrapper for an {@code AntibiogramReading} aggregate-internal entity. Cross-module
 * callers pass {@code AntibiogramReadingId} values; the underlying {@code String} ULID is used as
 * the JPA primary key.
 */
public record AntibiogramReadingId(String value) {

    public AntibiogramReadingId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static AntibiogramReadingId next() {
        return new AntibiogramReadingId(Ulid.generate());
    }
}
