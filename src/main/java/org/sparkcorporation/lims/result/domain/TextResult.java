package org.sparkcorporation.lims.result.domain;

import java.util.Objects;

/** Permit of {@link ResultValue} for catalog {@code ResultType.TEXT} (constrained text). */
public record TextResult(String value) implements ResultValue {

    public TextResult {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public String renderForReport() {
        return value;
    }
}
