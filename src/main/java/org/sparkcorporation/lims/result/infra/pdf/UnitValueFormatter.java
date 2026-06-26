package org.sparkcorporation.lims.result.infra.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jspecify.annotations.Nullable;

/**
 * Pure formatter for rendering numeric result values in their configured units (CDC §Unités, SP11).
 * Unit-1 values are formatted to the configured decimal places; unit-2 values are the unit-1 value
 * multiplied by the conversion factor, then formatted ({@code HALF_UP}). Used by the Compte-Rendu.
 */
public final class UnitValueFormatter {

    private static final int DEFAULT_DECIMALS = 2;

    private UnitValueFormatter() {
        throw new AssertionError("utility class");
    }

    /** Unit-1 value formatted to {@code decimals} places; {@code ""} when no value. */
    public static String unit1Value(@Nullable BigDecimal value, @Nullable Integer decimals) {
        if (value == null) {
            return "";
        }
        return decimals == null
                ? value.toPlainString()
                : value.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Unit-2 value = {@code value × factor} formatted to {@code decimals} places ({@code HALF_UP}).
     * {@code null} when no value or no factor (i.e. the analysis has no second unit configured).
     */
    public static @Nullable String unit2Value(
            @Nullable BigDecimal value, @Nullable BigDecimal factor, @Nullable Integer decimals) {
        if (value == null || factor == null) {
            return null;
        }
        int scale = decimals == null ? DEFAULT_DECIMALS : decimals;
        return value.multiply(factor).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }
}
