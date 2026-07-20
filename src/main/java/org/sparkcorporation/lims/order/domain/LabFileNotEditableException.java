package org.sparkcorporation.lims.order.domain;

/**
 * Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 5). Thrown when any mutator is invoked on
 * a {@link LabFile} whose {@code status != OPEN}.
 */
public class LabFileNotEditableException extends RuntimeException {

    public LabFileNotEditableException(String message) {
        super(message);
    }
}
