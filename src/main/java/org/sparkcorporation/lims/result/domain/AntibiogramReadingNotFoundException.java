package org.sparkcorporation.lims.result.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.result.api.ResultId;

/**
 * Thrown when an antibiogram reading cannot be located inside a parent {@code Result}. Routed to
 * {@code 404} via the shared advice {@link EntityNotFoundException} mapper.
 */
public class AntibiogramReadingNotFoundException extends EntityNotFoundException {

    public AntibiogramReadingNotFoundException(ResultId resultId, AntibiogramReadingId readingId) {
        super("Reading " + readingId.value() + " not found in Result " + resultId.value());
    }
}
