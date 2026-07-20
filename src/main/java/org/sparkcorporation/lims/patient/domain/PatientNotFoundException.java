package org.sparkcorporation.lims.patient.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Thrown by {@code PatientApi.requireById} when no patient matches. Mapped to 404 by
 * ProblemDetailAdvice.
 */
public class PatientNotFoundException extends EntityNotFoundException {

    public PatientNotFoundException(PatientId id) {
        super("patient not found: " + id.value());
    }
}
