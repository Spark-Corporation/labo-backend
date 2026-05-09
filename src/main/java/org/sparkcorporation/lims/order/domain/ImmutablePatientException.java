package org.sparkcorporation.lims.order.domain;

/**
 * Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 5). Thrown when a PATCH body contains the
 * {@code patientId} key — patient assignment is fixed at file open and cannot be changed.
 */
public class ImmutablePatientException extends RuntimeException {

    public ImmutablePatientException(String message) {
        super(message);
    }
}
