package org.sparkcorporation.lims.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SampleDailyIdTest {

    @Test
    void accepts_biochimie_prefix() {
        assertThat(new SampleDailyId("1070526-001").value()).isEqualTo("1070526-001");
    }

    @Test
    void accepts_biologie_moleculaire_prefix() {
        assertThat(new SampleDailyId("5311226-999").value()).isEqualTo("5311226-999");
    }

    @Test
    void rejects_zero_prefix() {
        assertThatThrownBy(() -> new SampleDailyId("0070526-001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_six_prefix() {
        assertThatThrownBy(() -> new SampleDailyId("6070526-001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_seven_prefix() {
        assertThatThrownBy(() -> new SampleDailyId("7070526-001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null_value() {
        assertThatThrownBy(() -> new SampleDailyId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid daily id");
    }

    @Test
    void rejects_wrong_sequence_length() {
        assertThatThrownBy(() -> new SampleDailyId("1070526-01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_few_date_digits() {
        assertThatThrownBy(() -> new SampleDailyId("105260-001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new SampleDailyId(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
