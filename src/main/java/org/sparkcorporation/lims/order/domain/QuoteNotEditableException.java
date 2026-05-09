package org.sparkcorporation.lims.order.domain;

/**
 * Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 3). Thrown when any DRAFT-only mutator is
 * invoked on a {@link Quote} whose status is not {@code DRAFT}, or when {@code :convert} is invoked
 * on a quote that is not {@code LOCKED} (and not already {@code CONVERTED} — that distinct case
 * raises {@link QuoteAlreadyConvertedException} via {@link
 * Quote#convert(org.sparkcorporation.lims.order.api.LabFileId, java.time.Clock)} precedence).
 */
public class QuoteNotEditableException extends RuntimeException {

    public QuoteNotEditableException(String detail) {
        super(detail);
    }
}
