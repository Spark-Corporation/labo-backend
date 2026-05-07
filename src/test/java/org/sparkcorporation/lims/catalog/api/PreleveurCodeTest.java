package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PreleveurCodeTest {

    @Test
    void accepts_auto_generated_format() {
        assertThat(PreleveurCode.of("PHL-000001").value()).isEqualTo("PHL-000001");
        assertThat(PreleveurCode.of("PHL-999999").value()).isEqualTo("PHL-999999");
    }

    @Test
    void rejects_wrong_prefix() {
        assertThatThrownBy(() -> PreleveurCode.of("PRS-000001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_lowercase_prefix() {
        assertThatThrownBy(() -> PreleveurCode.of("phl-000001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_few_digits() {
        assertThatThrownBy(() -> PreleveurCode.of("PHL-12345"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_many_digits() {
        assertThatThrownBy(() -> PreleveurCode.of("PHL-1234567"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_missing_separator() {
        assertThatThrownBy(() -> PreleveurCode.of("PHL000001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> PreleveurCode.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> PreleveurCode.of(null)).isInstanceOf(NullPointerException.class);
    }
}
