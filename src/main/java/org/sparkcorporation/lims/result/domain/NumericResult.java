package org.sparkcorporation.lims.result.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Permit of {@link ResultValue} for catalog {@code ResultType.REAL}.
 *
 * <p>Naming asymmetry per spec §16.7: catalog enum is {@code REAL} (CDC §VIII vocabulary "Réel");
 * domain permit is {@code NumericResult} (JVM type vocabulary). Reconciled at {@code @PostLoad}.
 */
public record NumericResult(BigDecimal value) implements ResultValue {

    public NumericResult {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public String renderForReport() {
        return value.toPlainString();
    }
}
