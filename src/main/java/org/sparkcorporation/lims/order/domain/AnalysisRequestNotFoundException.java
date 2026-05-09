package org.sparkcorporation.lims.order.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;

/** Mapped → 404 by {@code shared.web.ProblemDetailAdvice} via {@link EntityNotFoundException}. */
public class AnalysisRequestNotFoundException extends EntityNotFoundException {

    public AnalysisRequestNotFoundException(AnalysisRequestId id) {
        super("analysis request not found: " + id.value());
    }
}
