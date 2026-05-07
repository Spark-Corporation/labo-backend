package org.sparkcorporation.lims.catalog.domain;

import org.sparkcorporation.lims.shared.domain.PatientCategory;

/** Mapped → 422 by {@code CatalogProblemDetailAdvice} (Chunk 5). */
public class NoIntervalsToReplicateException extends RuntimeException {

    public NoIntervalsToReplicateException(PatientCategory sourceCategory) {
        super("No intervals to replicate for source category: " + sourceCategory);
    }

    public NoIntervalsToReplicateException(String message) {
        super(message);
    }
}
