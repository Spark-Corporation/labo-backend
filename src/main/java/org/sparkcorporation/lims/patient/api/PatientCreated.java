package org.sparkcorporation.lims.patient.api;

import java.time.Instant;
import java.util.Objects;

/** Domain event published after a patient creation transaction commits. */
public record PatientCreated(PatientId id, String labCode, Instant createdAt) {

    public PatientCreated {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(labCode, "labCode");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
