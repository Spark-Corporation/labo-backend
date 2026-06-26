package org.sparkcorporation.lims.result.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Permit of {@link ResultValue} for catalog {@code ResultType.FORMULA}. Server-computed numeric
 * value (CDC §Config.8 "Formule", SP11); persisted in {@code value_real}. {@code null} means the
 * formula could not be resolved (missing inputs / cycle) and renders blank.
 */
public record FormulaResult(@Nullable BigDecimal value) implements ResultValue {

    @Override
    public String renderForReport() {
        return value == null ? "" : value.toPlainString();
    }
}
