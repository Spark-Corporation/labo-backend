package org.sparkcorporation.lims.patient.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID identifier for a {@link org.sparkcorporation.lims.patient.domain.Patient}. */
public record PatientId(String value) {

    public PatientId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static PatientId next() {
        return new PatientId(Ulid.generate());
    }
}
