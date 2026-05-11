package org.sparkcorporation.lims.result.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.result.api.ResultId;

/**
 * Thrown when a {@code Result} cannot be located by id. Routed to {@code 404} via the shared {@code
 * ProblemDetailAdvice} {@link EntityNotFoundException} mapper.
 */
public class ResultNotFoundException extends EntityNotFoundException {

    public ResultNotFoundException(ResultId id) {
        super("Result " + id.value() + " not found");
    }
}
