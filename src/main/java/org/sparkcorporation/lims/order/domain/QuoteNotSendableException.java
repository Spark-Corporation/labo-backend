package org.sparkcorporation.lims.order.domain;

/**
 * Mapped → 409 by {@code OrderProblemDetailAdvice}. Thrown when {@code :send} is invoked on a quote
 * whose status is not {@code LOCKED} or {@code CONVERTED} (i.e. {@code DRAFT} or {@code EXPIRED}):
 * a quote that has not been priced-and-locked carries no contractual figures and must not be sent.
 */
public class QuoteNotSendableException extends RuntimeException {

    public QuoteNotSendableException(String detail) {
        super(detail);
    }
}
