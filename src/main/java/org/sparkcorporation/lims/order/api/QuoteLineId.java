package org.sparkcorporation.lims.order.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@code org.sparkcorporation.lims.order.domain.QuoteLine}. */
public record QuoteLineId(String value) {

    public QuoteLineId {
        Objects.requireNonNull(value, "QuoteLineId value must not be null");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static QuoteLineId next() {
        return new QuoteLineId(Ulid.generate());
    }
}
