package org.sparkcorporation.lims.result.domain;

import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultState;

/**
 * Thrown by {@link Result#delete} when the aggregate is not in {@code DRAFT}. Routed to {@code 409
 * result.not-deletable-in-state} by {@code ResultProblemDetailAdvice} (chunk 5).
 */
public class ResultNotDeletableInStateException extends RuntimeException {

    private final ResultId resultId;
    private final ResultState state;

    public ResultNotDeletableInStateException(ResultId id, ResultState state) {
        super(
                "Result "
                        + id.value()
                        + " in state "
                        + state
                        + " cannot be deleted; only DRAFT results may be deleted");
        this.resultId = id;
        this.state = state;
    }

    public ResultId resultId() {
        return resultId;
    }

    public ResultState state() {
        return state;
    }
}
