package org.sparkcorporation.lims.patient.domain;

/**
 * Thrown by {@code PatientService} when a create/update request references an {@code insuranceId}
 * that is not resolvable through {@code catalog::api} (missing or soft-deleted). Mapped to 400 by
 * {@code ProblemDetailAdvice}.
 */
public class PatientInvalidInsuranceReferenceException extends RuntimeException {

    public PatientInvalidInsuranceReferenceException(String insuranceId) {
        super("unknown insurance reference: " + insuranceId);
    }
}
