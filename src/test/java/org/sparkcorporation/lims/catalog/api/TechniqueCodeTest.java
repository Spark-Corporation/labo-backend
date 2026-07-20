package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TechniqueCodeTest {

    @Test
    void accepts_uppercase_with_underscores() {
        assertThat(new TechniqueCode("ELISA").value()).isEqualTo("ELISA");
        assertThat(new TechniqueCode("PCR_REAL_TIME").value()).isEqualTo("PCR_REAL_TIME");
        assertThat(new TechniqueCode("AB").value()).isEqualTo("AB");
    }

    @Test
    void rejects_lowercase() {
        assertThatThrownBy(() -> new TechniqueCode("elisa"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_starting_with_digit() {
        assertThatThrownBy(() -> new TechniqueCode("1AB"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> new TechniqueCode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new TechniqueCode("A" + "_".repeat(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new TechniqueCode(null)).isInstanceOf(NullPointerException.class);
    }
}
