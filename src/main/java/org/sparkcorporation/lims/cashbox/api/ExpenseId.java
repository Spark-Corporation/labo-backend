package org.sparkcorporation.lims.cashbox.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

public record ExpenseId(String value) {

    public ExpenseId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static ExpenseId generate() {
        return new ExpenseId(Ulid.generate());
    }

    public static ExpenseId of(String value) {
        return new ExpenseId(value);
    }
}
