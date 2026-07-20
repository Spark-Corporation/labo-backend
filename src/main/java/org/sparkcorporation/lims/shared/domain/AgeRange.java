package org.sparkcorporation.lims.shared.domain;

import java.util.Objects;

/**
 * Inclusive age range used by reference-interval definitions. Stored as three columns in {@code
 * reference_intervals(age_min, age_max, age_unit)} per LIMS Tech Doc §6.3.
 *
 * <p>Cross-unit comparison via {@link #contains(Age)} uses {@link Age#toDays()} so a range
 * expressed in {@code MONTHS} can correctly classify an age expressed in {@code DAYS} or {@code
 * YEARS}.
 */
public record AgeRange(int min, int max, AgeUnit unit) {

    public AgeRange {
        if (min < 0) {
            throw new IllegalArgumentException("min must be ≥ 0, got " + min);
        }
        if (max < min) {
            throw new IllegalArgumentException("max must be ≥ min, got min=" + min + " max=" + max);
        }
        Objects.requireNonNull(unit, "unit");
    }

    /** Convenience: a single age point as a degenerate range {@code [n, n]}. */
    public static AgeRange exactly(int n, AgeUnit unit) {
        return new AgeRange(n, n, unit);
    }

    /** Open upper bound — used when a range applies "from N onwards". */
    public static AgeRange from(int min, AgeUnit unit) {
        return new AgeRange(min, Integer.MAX_VALUE, unit);
    }

    public long minDays() {
        return new Age(min, unit).toDays();
    }

    public long maxDays() {
        return max == Integer.MAX_VALUE ? Long.MAX_VALUE : new Age(max, unit).toDays();
    }

    public boolean contains(Age age) {
        long d = age.toDays();
        return d >= minDays() && d <= maxDays();
    }
}
