package org.sparkcorporation.lims.billing.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.billing.domain.Invoice}. */
public record InvoiceId(String value) {

    public InvoiceId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static InvoiceId generate() {
        return new InvoiceId(Ulid.generate());
    }

    public static InvoiceId of(String value) {
        return new InvoiceId(value);
    }
}
