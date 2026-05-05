package org.sparkcorporation.lims.patient.api;

import java.time.Instant;
import java.util.Objects;

/** Domain event published after a patient merge transaction commits. */
public record PatientMerged(PatientId source, PatientId target, Instant mergedAt) {

    public PatientMerged {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(mergedAt, "mergedAt");
        if (source.equals(target)) {
            throw new IllegalArgumentException("cannot merge a patient into itself");
        }
    }
}
