package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AgeTest {

    @Test
    void rejects_negative_value() {
        assertThatThrownBy(() -> new Age(-1, AgeUnit.YEARS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null_unit() {
        assertThatThrownBy(() -> new Age(0, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void allows_zero_value() {
        assertThat(new Age(0, AgeUnit.DAYS).value()).isZero();
    }

    @Test
    void toDays_for_DAYS_is_identity() {
        assertThat(new Age(7, AgeUnit.DAYS).toDays()).isEqualTo(7L);
    }

    @Test
    void toDays_for_MONTHS_uses_30_day_month() {
        assertThat(new Age(1, AgeUnit.MONTHS).toDays()).isEqualTo(30L);
        assertThat(new Age(12, AgeUnit.MONTHS).toDays()).isEqualTo(360L);
    }

    @Test
    void toDays_for_YEARS_uses_365_day_year() {
        assertThat(new Age(1, AgeUnit.YEARS).toDays()).isEqualTo(365L);
        assertThat(new Age(18, AgeUnit.YEARS).toDays()).isEqualTo(6570L);
    }

    @Test
    void toDays_for_max_YEARS_does_not_overflow() {
        long expected = (long) Integer.MAX_VALUE * 365L;
        assertThat(new Age(Integer.MAX_VALUE, AgeUnit.YEARS).toDays()).isEqualTo(expected);
    }

    @Test
    void equals_null_is_false() {
        Age a = new Age(5, AgeUnit.YEARS);
        assertThat(a.equals(null)).isFalse();
    }

    @Test
    void record_equality_holds() {
        assertThat(new Age(5, AgeUnit.YEARS)).isEqualTo(new Age(5, AgeUnit.YEARS));
        assertThat(new Age(5, AgeUnit.YEARS)).isNotEqualTo(new Age(5, AgeUnit.MONTHS));
    }
}
