package org.sparkcorporation.lims.cashbox.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

public record HomeCollectionFeeId(String value) {

    public HomeCollectionFeeId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static HomeCollectionFeeId generate() {
        return new HomeCollectionFeeId(Ulid.generate());
    }

    public static HomeCollectionFeeId of(String value) {
        return new HomeCollectionFeeId(value);
    }
}
