package org.sparkcorporation.lims.result.domain;

import java.util.Objects;

/** Permit of {@link ResultValue} for catalog {@code ResultType.FREE} ("Autre"). */
public record FreeFormResult(String value) implements ResultValue {

    public FreeFormResult {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public String renderForReport() {
        return value;
    }
}
