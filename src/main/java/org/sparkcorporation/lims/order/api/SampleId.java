package org.sparkcorporation.lims.order.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for an order Sample (sealed hierarchy persisted in {@code samples}). */
public record SampleId(String value) {

    public SampleId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static SampleId next() {
        return new SampleId(Ulid.generate());
    }
}
