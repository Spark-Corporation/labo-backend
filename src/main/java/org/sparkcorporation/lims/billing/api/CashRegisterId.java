package org.sparkcorporation.lims.billing.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.billing.domain.CashRegister}. */
public record CashRegisterId(String value) {

    public CashRegisterId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static CashRegisterId generate() {
        return new CashRegisterId(Ulid.generate());
    }

    public static CashRegisterId of(String value) {
        return new CashRegisterId(value);
    }
}
