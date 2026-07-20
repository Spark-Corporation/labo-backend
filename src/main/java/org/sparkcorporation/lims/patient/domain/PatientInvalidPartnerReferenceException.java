package org.sparkcorporation.lims.patient.domain;

/**
 * Thrown by {@code PatientService} when a create/update request references a {@code partnerId} that
 * is not resolvable through {@code catalog::api} (missing or soft-deleted). Mapped to 400 by {@code
 * ProblemDetailAdvice}.
 */
public class PatientInvalidPartnerReferenceException extends RuntimeException {

    public PatientInvalidPartnerReferenceException(String partnerId) {
        super("unknown partner reference: " + partnerId);
    }
}
