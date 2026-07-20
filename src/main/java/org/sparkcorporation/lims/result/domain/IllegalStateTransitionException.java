package org.sparkcorporation.lims.result.domain;

import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultState;

/**
 * Thrown by {@link Result#validateTechnical} / {@link Result#validateBiological} when the requested
 * transition does not satisfy the forward-only state machine. Routed to {@code 409
 * result.illegal-transition} by {@code ResultProblemDetailAdvice} (chunk 5).
 */
public class IllegalStateTransitionException extends RuntimeException {

    private final ResultId resultId;
    private final ResultState currentState;
    private final ResultState targetState;

    public IllegalStateTransitionException(ResultId id, ResultState current, ResultState target) {
        super("Cannot transition Result " + id.value() + " from " + current + " to " + target);
        this.resultId = id;
        this.currentState = current;
        this.targetState = target;
    }

    public ResultId resultId() {
        return resultId;
    }

    public ResultState currentState() {
        return currentState;
    }

    public ResultState targetState() {
        return targetState;
    }
}
