package org.sparkcorporation.lims.result.domain;

import java.util.Objects;

/**
 * Permit of {@link ResultValue} for catalog {@code ResultType.FORMULA}. Holds the pre-rendered
 * string the client computed from the formula expression — server stores it verbatim.
 */
public record FormulaResult(String renderedValue) implements ResultValue {

    public FormulaResult {
        Objects.requireNonNull(renderedValue, "renderedValue");
    }

    @Override
    public String renderForReport() {
        return renderedValue;
    }
}
