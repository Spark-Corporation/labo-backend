package org.sparkcorporation.lims.order.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@code org.sparkcorporation.lims.order.domain.Quote}. */
public record QuoteId(String value) {

    public QuoteId {
        Objects.requireNonNull(value, "QuoteId value must not be null");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static QuoteId next() {
        return new QuoteId(Ulid.generate());
    }
}
