package org.sparkcorporation.lims.order.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.order.api.QuoteLineId;

/** Mapped → 404 by {@code shared.web.ProblemDetailAdvice} via {@link EntityNotFoundException}. */
public class QuoteLineNotFoundException extends EntityNotFoundException {

    public QuoteLineNotFoundException(QuoteLineId id) {
        super("Quote line not found: " + id.value());
    }
}
