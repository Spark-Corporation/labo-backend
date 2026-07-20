package org.sparkcorporation.lims.billing.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.billing.domain.Payment}. */
public record PaymentId(String value) {

    public PaymentId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static PaymentId generate() {
        return new PaymentId(Ulid.generate());
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
    }
}
