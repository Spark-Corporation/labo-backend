package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AntibioticCodeTest {

    @Test
    void accepts_uppercase_with_underscores() {
        assertThat(new AntibioticCode("AMOXICILLIN").value()).isEqualTo("AMOXICILLIN");
        assertThat(new AntibioticCode("CEFTRIAXONE_IV").value()).isEqualTo("CEFTRIAXONE_IV");
        assertThat(new AntibioticCode("AB").value()).isEqualTo("AB");
    }

    @Test
    void rejects_lowercase() {
        assertThatThrownBy(() -> new AntibioticCode("amoxicillin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_starting_with_digit() {
        assertThatThrownBy(() -> new AntibioticCode("1AB"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> new AntibioticCode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new AntibioticCode("A" + "_".repeat(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new AntibioticCode(null)).isInstanceOf(NullPointerException.class);
    }
}
