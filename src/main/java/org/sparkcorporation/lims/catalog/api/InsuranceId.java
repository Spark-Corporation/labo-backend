package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.catalog.domain.Insurance}. */
public record InsuranceId(String value) {

    public InsuranceId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static InsuranceId generate() {
        return new InsuranceId(Ulid.generate());
    }

    public static InsuranceId of(String value) {
        return new InsuranceId(value);
    }
}
