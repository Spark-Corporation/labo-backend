package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LabCodePrefixTest {

    @Test
    void accepts_2_to_5_uppercase_letters() {
        assertThat(new LabCodePrefix("LB").value()).isEqualTo("LB");
        assertThat(new LabCodePrefix("LAB").value()).isEqualTo("LAB");
        assertThat(new LabCodePrefix("ABCDE").value()).isEqualTo("ABCDE");
    }

    @Test
    void rejects_too_short() {
        assertThatThrownBy(() -> new LabCodePrefix("A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new LabCodePrefix("ABCDEF"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_lowercase() {
        assertThatThrownBy(() -> new LabCodePrefix("lab"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_digits() {
        assertThatThrownBy(() -> new LabCodePrefix("L4B"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new LabCodePrefix(null)).isInstanceOf(NullPointerException.class);
    }
}
