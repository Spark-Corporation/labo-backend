package org.sparkcorporation.lims.result.domain;

/**
 * Domain-side sealed type per tech doc §5.5. <strong>Not</strong> a JPA entity hierarchy — the
 * persistence side stores polymorphic typed columns ({@code value_integer / value_real /
 * value_text}) and reconstructs the right permit in {@link ResultLine#rehydrateValue()}. This keeps
 * Hibernate proxy generation off the sealed type (we hit that issue with {@code Sample} in 1.4).
 */
public sealed interface ResultValue
        permits IntegerResult, NumericResult, TextResult, FormulaResult, FreeFormResult {

    /** Renders the value as a single string for inclusion in printed reports. */
    String renderForReport();
}
