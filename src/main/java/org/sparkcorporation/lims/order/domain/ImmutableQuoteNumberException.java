package org.sparkcorporation.lims.order.domain;

/**
 * Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 3). Thrown when a PATCH body attempts to
 * change {@code quoteNumber} (immutable per spec §5.2).
 */
public class ImmutableQuoteNumberException extends RuntimeException {

    public ImmutableQuoteNumberException(String detail) {
        super(detail);
    }
}
