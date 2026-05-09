package org.sparkcorporation.lims.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LabFileRegistrationNumberTest {

    @Test
    void accepts_canonical_format() {
        assertThat(new LabFileRegistrationNumber("LF-260507-0001").value())
                .isEqualTo("LF-260507-0001");
    }

    @Test
    void accepts_max_four_digit_sequence() {
        assertThat(new LabFileRegistrationNumber("LF-991231-9999").value())
                .isEqualTo("LF-991231-9999");
    }

    @Test
    void rejects_null_value() {
        assertThatThrownBy(() -> new LabFileRegistrationNumber(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid registration number");
    }

    @Test
    void rejects_wrong_prefix() {
        assertThatThrownBy(() -> new LabFileRegistrationNumber("XX-260507-0001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_few_date_digits() {
        assertThatThrownBy(() -> new LabFileRegistrationNumber("LF-26057-0001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_few_sequence_digits() {
        assertThatThrownBy(() -> new LabFileRegistrationNumber("LF-260507-001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_empty_string() {
        assertThatThrownBy(() -> new LabFileRegistrationNumber(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
