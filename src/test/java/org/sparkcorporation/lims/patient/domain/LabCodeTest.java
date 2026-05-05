package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LabCodeTest {

    @Test
    void accepts_typical_3_digit_form() {
        assertThat(new LabCode("LAB-260505-001").value()).isEqualTo("LAB-260505-001");
    }

    @Test
    void accepts_4_digit_busy_day_form() {
        assertThat(new LabCode("LAB-260505-1234").value()).isEqualTo("LAB-260505-1234");
    }

    @Test
    void accepts_5_digit_extreme_form() {
        assertThat(new LabCode("ABCDE-260505-99999").value()).isEqualTo("ABCDE-260505-99999");
    }

    @Test
    void rejects_unpadded_2_digit_seq() {
        assertThatThrownBy(() -> new LabCode("LAB-260505-12"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_lowercase_prefix() {
        assertThatThrownBy(() -> new LabCode("lab-260505-001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_wrong_separator() {
        assertThatThrownBy(() -> new LabCode("LAB_260505_001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new LabCode(null)).isInstanceOf(NullPointerException.class);
    }
}
