package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.catalog.api.AnalysisId;

/** Mapped → 404 by {@code shared.web.ProblemDetailAdvice} via {@link EntityNotFoundException}. */
public class AnalysisNotFoundException extends EntityNotFoundException {

    public AnalysisNotFoundException(AnalysisId id) {
        super("Analysis not found: " + id.value());
    }
}
