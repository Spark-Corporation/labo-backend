package org.sparkcorporation.lims.order.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.order.api.SampleId;

/** Mapped → 404 by {@code shared.web.ProblemDetailAdvice} via {@link EntityNotFoundException}. */
public class SampleNotFoundException extends EntityNotFoundException {

    public SampleNotFoundException(SampleId id) {
        super("sample not found: " + id.value());
    }
}
