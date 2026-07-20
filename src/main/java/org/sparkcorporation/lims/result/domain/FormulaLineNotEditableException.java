package org.sparkcorporation.lims.result.domain;

import org.sparkcorporation.lims.result.api.ResultLineId;

/**
 * Thrown by {@code ResultLine#setValue} when an operator tries to set a value on a {@code
 * ResultType.FORMULA} line. Formule results are server-computed from sibling values (CDC §Config.8,
 * SP11) — never operator-entered. Routed to {@code 422 result-line.formula-not-editable} by {@code
 * ResultProblemDetailAdvice}, mirroring {@link ResultLineKindMismatchException}.
 */
public class FormulaLineNotEditableException extends RuntimeException {

    private final ResultLineId lineId;

    public FormulaLineNotEditableException(ResultLineId lineId) {
        super("Line " + lineId.value() + " is a FORMULA line; its value is server-computed");
        this.lineId = lineId;
    }

    public ResultLineId lineId() {
        return lineId;
    }
}
