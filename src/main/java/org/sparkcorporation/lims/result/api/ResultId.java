package org.sparkcorporation.lims.result.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Typed ULID wrapper for the {@code Result} aggregate root. Cross-module callers pass {@code
 * ResultId} values; the underlying {@code String} ULID is used as the JPA primary key.
 */
public record ResultId(String value) {

    public ResultId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static ResultId next() {
        return new ResultId(Ulid.generate());
    }
}
