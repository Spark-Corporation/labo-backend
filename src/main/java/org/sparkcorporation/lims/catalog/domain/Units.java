package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

/**
 * Embedded value object on the {@code Analysis} aggregate per CDC §8 onglet "Unités".
 *
 * <p>Mapped to columns {@code unit_1, unit_1_decimals, unit_2, unit_2_decimals, unit_1_to_2_factor}
 * — explicit overrides because the field-name → column-name mapping is not pure camelCase ↔
 * snake_case ({@code conversionFactor} ↔ {@code unit_1_to_2_factor}). All fields are nullable.
 *
 * <p>Validation: {@code unit1Decimals} and {@code unit2Decimals} must be in {@code [0, 5]} per CDC.
 */
@Embeddable
public record Units(
        @Column(name = "unit_1") @Nullable String unit1,
        @Column(name = "unit_1_decimals") @Nullable Integer unit1Decimals,
        @Column(name = "unit_2") @Nullable String unit2,
        @Column(name = "unit_2_decimals") @Nullable Integer unit2Decimals,
        @Column(name = "unit_1_to_2_factor") @Nullable BigDecimal conversionFactor) {

    private static final int MAX_DECIMALS = 5;

    public Units {
        if (unit1Decimals != null && (unit1Decimals < 0 || unit1Decimals > MAX_DECIMALS)) {
            throw new IllegalArgumentException(
                    "unit1Decimals must be in [0, " + MAX_DECIMALS + "]");
        }
        if (unit2Decimals != null && (unit2Decimals < 0 || unit2Decimals > MAX_DECIMALS)) {
            throw new IllegalArgumentException(
                    "unit2Decimals must be in [0, " + MAX_DECIMALS + "]");
        }
    }

    /** Empty Units (all fields null) — used when an Analysis has no Units configured. */
    public static Units empty() {
        return new Units(null, null, null, null, null);
    }
}
