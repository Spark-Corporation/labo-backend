package org.sparkcorporation.lims.cashbox.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

public record JournalEntryId(String value) {

    public JournalEntryId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static JournalEntryId generate() {
        return new JournalEntryId(Ulid.generate());
    }

    public static JournalEntryId of(String value) {
        return new JournalEntryId(value);
    }
}
