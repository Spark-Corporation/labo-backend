package org.sparkcorporation.lims.result.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Typed ULID wrapper for a {@code ResultLine} aggregate-internal entity. Cross-module callers pass
 * {@code ResultLineId} values; the underlying {@code String} ULID is used as the JPA primary key.
 */
public record ResultLineId(String value) {

    public ResultLineId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static ResultLineId next() {
        return new ResultLineId(Ulid.generate());
    }
}
