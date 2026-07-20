package org.sparkcorporation.lims.order.domain;

import org.sparkcorporation.lims.order.api.QuoteId;

/**
 * Mapped → 422 by {@code OrderProblemDetailAdvice} (Chunk 3). Distinct from {@link
 * QuoteNotEditableException} so the client can render a clearer error UX (the surviving {@code
 * labFileId} is exposed via {@link #labFileId()}).
 *
 * <p>Per spec §5.5: {@code Quote.convert} checks the {@code CONVERTED} branch BEFORE the generic
 * {@code != LOCKED} branch — exception precedence is part of the contract.
 */
public class QuoteAlreadyConvertedException extends RuntimeException {

    private final String labFileId;

    public QuoteAlreadyConvertedException(QuoteId id, String labFileId) {
        super("Quote " + id.value() + " is already CONVERTED to lab file " + labFileId);
        this.labFileId = labFileId;
    }

    public String labFileId() {
        return labFileId;
    }
}
