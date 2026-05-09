package org.sparkcorporation.lims.order.domain;

/**
 * Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 5). Thrown when a PATCH body contains the
 * {@code registrationNumber} key — the registration number is generated at file open and never
 * mutated thereafter.
 */
public class ImmutableRegistrationNumberException extends RuntimeException {

    public ImmutableRegistrationNumberException(String message) {
        super(message);
    }
}
