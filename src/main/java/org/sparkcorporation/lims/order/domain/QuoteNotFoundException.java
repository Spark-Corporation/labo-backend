package org.sparkcorporation.lims.order.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.order.api.QuoteId;

/** Mapped → 404 by {@code shared.web.ProblemDetailAdvice} via {@link EntityNotFoundException}. */
public class QuoteNotFoundException extends EntityNotFoundException {

    public QuoteNotFoundException(QuoteId id) {
        super("Quote not found: " + id.value());
    }
}
