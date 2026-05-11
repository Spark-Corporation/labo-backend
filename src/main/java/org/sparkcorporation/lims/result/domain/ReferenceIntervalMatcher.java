package org.sparkcorporation.lims.result.domain;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Pure function: given a patient's category/sex/age and a list of candidate reference intervals,
 * picks the most specific match and computes the {@code isOutOfRange} flag for a line value.
 *
 * <p>Algorithm per spec §5.2:
 *
 * <ol>
 *   <li>Filter by category equality.
 *   <li>Filter by sex (candidate {@code sex == null} is wildcard).
 *   <li>Filter by age window inclusive (open-upper if {@code ageMax} is null).
 *   <li>Pick the most specific: prefer sex-specific over null-sex; then prefer narrower age window;
 *       tie-break by id ASC for determinism.
 * </ol>
 *
 * <p>{@code isOutOfRange} per spec §5.3: only when {@code mode == STANDARD} and the value is
 * outside {@code [unit_1_min, unit_1_max]}. {@code COMMENT} and {@code NONE} modes are never
 * out-of-range. {@code INTEGER} widens to {@link BigDecimal}; {@code TEXT/FORMULA/FREE} skip
 * scoring.
 */
public final class ReferenceIntervalMatcher {

    /**
     * Calendar-blind days per month for cross-unit comparison; matches {@code shared.domain.Age}.
     */
    private static final long DAYS_PER_MONTH = 30L;

    /**
     * Calendar-blind days per year for cross-unit comparison; matches {@code shared.domain.Age}.
     */
    private static final long DAYS_PER_YEAR = 365L;

    private ReferenceIntervalMatcher() {
        throw new AssertionError("utility class");
    }

    public static Optional<ReferenceIntervalSummary> match(
            PatientCategory category,
            Sex sex,
            @Nullable Integer ageInDays,
            List<ReferenceIntervalSummary> candidates) {
        if (ageInDays == null) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(ri -> ri.category() == category)
                .filter(ri -> ri.sex() == null || ri.sex() == sex)
                .filter(ri -> ageMatches(ri, ageInDays))
                .min(
                        Comparator
                                // sex-specific (false) sorts before null-sex (true)
                                .comparing((ReferenceIntervalSummary ri) -> ri.sex() == null)
                                .thenComparingLong(ReferenceIntervalMatcher::ageWindowDays)
                                .thenComparing(ri -> ri.id().value()));
    }

    public static boolean isOutOfRange(
            ResultType resultType,
            @Nullable Long valueInteger,
            @Nullable BigDecimal valueReal,
            @Nullable String valueText,
            @Nullable ReferenceIntervalSummary ri) {
        if (ri == null) {
            return false;
        }
        if (ri.mode() != ReferenceMode.STANDARD) {
            return false;
        }
        BigDecimal v =
                switch (resultType) {
                    case INTEGER -> valueInteger == null ? null : new BigDecimal(valueInteger);
                    case REAL -> valueReal;
                    case TEXT, FORMULA, FREE -> null;
                };
        if (v == null) {
            return false;
        }
        if (ri.unit1Min() != null && v.compareTo(ri.unit1Min()) < 0) {
            return true;
        }
        if (ri.unit1Max() != null && v.compareTo(ri.unit1Max()) > 0) {
            return true;
        }
        return false;
    }

    private static boolean ageMatches(ReferenceIntervalSummary ri, int ageInDays) {
        Integer min = ri.ageMin();
        Integer max = ri.ageMax();
        AgeUnit unit = ri.ageUnit();
        if (min == null && max == null) {
            return true;
        }
        long minDays = min == null ? Long.MIN_VALUE : toDays(min, unit);
        long maxDays = max == null ? Long.MAX_VALUE : toDays(max, unit);
        return ageInDays >= minDays && ageInDays <= maxDays;
    }

    private static long toDays(int value, @Nullable AgeUnit unit) {
        if (unit == null) {
            return value;
        }
        return switch (unit) {
            case DAYS -> value;
            case MONTHS -> (long) value * DAYS_PER_MONTH;
            case YEARS -> (long) value * DAYS_PER_YEAR;
        };
    }

    private static long ageWindowDays(ReferenceIntervalSummary ri) {
        long min = ri.ageMin() == null ? 0L : toDays(ri.ageMin(), ri.ageUnit());
        long max = ri.ageMax() == null ? Long.MAX_VALUE : toDays(ri.ageMax(), ri.ageUnit());
        // guard against overflow when subtracting MAX_VALUE
        if (max == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return max - min;
    }
}
