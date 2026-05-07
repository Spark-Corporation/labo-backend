package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SpecialtyCodeTest {

    @Test
    void accepts_admin_typed_uppercase_alphanumeric() {
        assertThat(SpecialtyCode.of("CARDIOLOGY").value()).isEqualTo("CARDIOLOGY");
        assertThat(SpecialtyCode.of("GENERAL_MEDICINE").value()).isEqualTo("GENERAL_MEDICINE");
    }

    @Test
    void rejects_lowercase() {
        assertThatThrownBy(() -> SpecialtyCode.of("cardiology"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_starting_digit() {
        assertThatThrownBy(() -> SpecialtyCode.of("1CARDIO"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> SpecialtyCode.of("A".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> SpecialtyCode.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> SpecialtyCode.of(null)).isInstanceOf(NullPointerException.class);
    }
}
