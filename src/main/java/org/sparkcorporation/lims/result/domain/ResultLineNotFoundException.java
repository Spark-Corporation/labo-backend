package org.sparkcorporation.lims.result.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;

/**
 * Thrown when a line cannot be located inside a parent {@code Result}. Routed to {@code 404} via
 * the shared advice {@link EntityNotFoundException} mapper.
 */
public class ResultLineNotFoundException extends EntityNotFoundException {

    public ResultLineNotFoundException(ResultId resultId, ResultLineId lineId) {
        super("Line " + lineId.value() + " not found in Result " + resultId.value());
    }
}
