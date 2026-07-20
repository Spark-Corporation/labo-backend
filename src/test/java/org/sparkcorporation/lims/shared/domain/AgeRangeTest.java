package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AgeRangeTest {

    @Test
    void rejects_negative_min() {
        assertThatThrownBy(() -> new AgeRange(-1, 5, AgeUnit.YEARS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_max_below_min() {
        assertThatThrownBy(() -> new AgeRange(5, 4, AgeUnit.YEARS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null_unit() {
        assertThatThrownBy(() -> new AgeRange(0, 5, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void exactly_yields_degenerate_range() {
        AgeRange r = AgeRange.exactly(7, AgeUnit.DAYS);
        assertThat(r.min()).isEqualTo(7);
        assertThat(r.max()).isEqualTo(7);
        assertThat(r.unit()).isEqualTo(AgeUnit.DAYS);
    }

    @Test
    void from_yields_open_upper_bound() {
        AgeRange r = AgeRange.from(18, AgeUnit.YEARS);
        assertThat(r.min()).isEqualTo(18);
        assertThat(r.max()).isEqualTo(Integer.MAX_VALUE);
        assertThat(r.maxDays()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void contains_within_range() {
        AgeRange r = new AgeRange(1, 10, AgeUnit.YEARS);
        assertThat(r.contains(new Age(5, AgeUnit.YEARS))).isTrue();
    }

    @Test
    void contains_at_lower_boundary_inclusive() {
        AgeRange r = new AgeRange(1, 10, AgeUnit.YEARS);
        assertThat(r.contains(new Age(1, AgeUnit.YEARS))).isTrue();
    }

    @Test
    void contains_at_upper_boundary_inclusive() {
        AgeRange r = new AgeRange(1, 10, AgeUnit.YEARS);
        assertThat(r.contains(new Age(10, AgeUnit.YEARS))).isTrue();
    }

    @Test
    void contains_below_range_returns_false() {
        AgeRange r = new AgeRange(1, 10, AgeUnit.YEARS);
        assertThat(r.contains(new Age(0, AgeUnit.YEARS))).isFalse();
    }

    @Test
    void contains_above_range_returns_false() {
        AgeRange r = new AgeRange(1, 10, AgeUnit.YEARS);
        assertThat(r.contains(new Age(11, AgeUnit.YEARS))).isFalse();
    }

    @Test
    void cross_unit_DAYS_in_MONTHS_range() {
        AgeRange r = new AgeRange(1, 12, AgeUnit.MONTHS);
        assertThat(r.contains(new Age(30, AgeUnit.DAYS))).isTrue();
    }

    @Test
    void cross_unit_MONTHS_in_YEARS_range_within_bound() {
        AgeRange r = new AgeRange(0, 1, AgeUnit.YEARS);
        assertThat(r.contains(new Age(12, AgeUnit.MONTHS))).isTrue();
    }

    @Test
    void cross_unit_MONTHS_above_YEARS_range() {
        AgeRange r = new AgeRange(0, 1, AgeUnit.YEARS);
        assertThat(r.contains(new Age(13, AgeUnit.MONTHS))).isFalse();
    }

    @Test
    void from_18_YEARS_contains_age_above_18() {
        AgeRange adult = AgeRange.from(18, AgeUnit.YEARS);
        assertThat(adult.contains(new Age(120, AgeUnit.YEARS))).isTrue();
    }

    @Test
    void equals_null_is_false() {
        AgeRange r = new AgeRange(0, 1, AgeUnit.YEARS);
        assertThat(r.equals(null)).isFalse();
    }
}
