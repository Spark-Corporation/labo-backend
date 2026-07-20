package org.sparkcorporation.lims.order.domain;

/** Patient ID supplied at file open did not resolve via {@code PatientApi.existsById}. */
public final class OrderInvalidPatientReferenceException extends OrderInvalidReferenceException {

    public OrderInvalidPatientReferenceException(String patientId) {
        super("patient", patientId, "patient not found: " + patientId);
    }
}
