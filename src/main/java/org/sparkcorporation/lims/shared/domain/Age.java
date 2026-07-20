package org.sparkcorporation.lims.shared.domain;

import java.util.Objects;

/**
 * A patient's age, stored at the precision the user entered. Used as input to {@link
 * PatientCategory#of(Sex, Age)} and {@link AgeRange#contains(Age)}.
 *
 * <p>Cross-unit comparison via {@link #toDays()} uses calendar-blind constants: 1 month = 30 days,
 * 1 year = 365 days. This is intentional simplification — medical-record category boundaries do not
 * depend on whether a child crossed February 29.
 */
public record Age(int value, AgeUnit unit) {

    /** Calendar-blind days per month used for cross-unit comparison. */
    private static final long DAYS_PER_MONTH = 30L;

    /** Calendar-blind days per year used for cross-unit comparison. */
    private static final long DAYS_PER_YEAR = 365L;

    public Age {
        if (value < 0) {
            throw new IllegalArgumentException("age value must be ≥ 0, got " + value);
        }
        Objects.requireNonNull(unit, "unit");
    }

    /** Canonical conversion to days for cross-unit comparison. Uses {@code long} arithmetic. */
    public long toDays() {
        return switch (unit) {
            case DAYS -> value;
            case MONTHS -> value * DAYS_PER_MONTH;
            case YEARS -> value * DAYS_PER_YEAR;
        };
    }
}
