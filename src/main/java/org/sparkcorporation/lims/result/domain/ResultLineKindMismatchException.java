package org.sparkcorporation.lims.result.domain;

import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.result.api.ResultLineId;

/**
 * Thrown by {@code ResultLine#setValue} when the {@link ResultValue} kind on the request does not
 * match the line's frozen {@link ResultType}. Routed to {@code 422 result-line.kind-mismatch} by
 * {@code ResultProblemDetailAdvice} (chunk 5).
 */
public class ResultLineKindMismatchException extends RuntimeException {

    private final ResultLineId lineId;
    private final ResultType lineType;
    private final String requestKind;

    public ResultLineKindMismatchException(
            ResultLineId lineId, ResultType lineType, String requestKind) {
        super(
                "Line "
                        + lineId.value()
                        + " has resultType="
                        + lineType
                        + "; received value of kind "
                        + requestKind);
        this.lineId = lineId;
        this.lineType = lineType;
        this.requestKind = requestKind;
    }

    public ResultLineId lineId() {
        return lineId;
    }

    public ResultType lineType() {
        return lineType;
    }

    public String requestKind() {
        return requestKind;
    }
}
