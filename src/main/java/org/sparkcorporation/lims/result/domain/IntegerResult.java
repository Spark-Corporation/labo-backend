package org.sparkcorporation.lims.result.domain;

/** Permit of {@link ResultValue} for catalog {@code ResultType.INTEGER}. */
public record IntegerResult(long value) implements ResultValue {

    @Override
    public String renderForReport() {
        return Long.toString(value);
    }
}
