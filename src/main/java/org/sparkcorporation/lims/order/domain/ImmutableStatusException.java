package org.sparkcorporation.lims.order.domain;

/**
 * Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 5). Thrown when a PATCH body contains the
 * {@code status} key — status transitions belong to dedicated workflow endpoints, not the generic
 * patch path.
 */
public class ImmutableStatusException extends RuntimeException {

    public ImmutableStatusException(String message) {
        super(message);
    }
}
