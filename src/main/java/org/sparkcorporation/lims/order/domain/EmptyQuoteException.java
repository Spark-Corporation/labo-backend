package org.sparkcorporation.lims.order.domain;

/**
 * Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 3). Thrown when {@code :lock} is invoked
 * on a {@link Quote} with no lines.
 */
public class EmptyQuoteException extends RuntimeException {

    public EmptyQuoteException(String detail) {
        super(detail);
    }
}
