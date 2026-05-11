package org.sparkcorporation.lims.result.domain;

import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultState;

/**
 * Thrown when any mutation is attempted on a {@code Result} that has reached {@code
 * TECHNICAL_VALIDATED} or {@code BIOLOGICAL_VALIDATED}. Routed to {@code 409 result.locked} by
 * {@code ResultProblemDetailAdvice} (chunk 5).
 */
public class ResultLockedException extends RuntimeException {

    private final ResultId resultId;
    private final ResultState state;

    public ResultLockedException(ResultId id, ResultState state) {
        super(
                "Result "
                        + id.value()
                        + " is in "
                        + state
                        + "; mutations not allowed past TECHNICAL_VALIDATED");
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
