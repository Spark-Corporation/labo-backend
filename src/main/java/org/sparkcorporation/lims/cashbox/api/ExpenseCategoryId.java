package org.sparkcorporation.lims.cashbox.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.cashbox.domain.ExpenseCategory}. */
public record ExpenseCategoryId(String value) {

    public ExpenseCategoryId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static ExpenseCategoryId generate() {
        return new ExpenseCategoryId(Ulid.generate());
    }

    public static ExpenseCategoryId of(String value) {
        return new ExpenseCategoryId(value);
    }
}
