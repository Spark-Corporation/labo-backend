package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MatrixCodeTest {

    @Test
    void accepts_uppercase_with_underscores() {
        assertThat(new MatrixCode("SERUM").value()).isEqualTo("SERUM");
        assertThat(new MatrixCode("WHOLE_BLOOD").value()).isEqualTo("WHOLE_BLOOD");
        assertThat(new MatrixCode("AB").value()).isEqualTo("AB");
    }

    @Test
    void rejects_lowercase() {
        assertThatThrownBy(() -> new MatrixCode("serum"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_starting_with_digit() {
        assertThatThrownBy(() -> new MatrixCode("1AB"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> new MatrixCode("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new MatrixCode("A" + "_".repeat(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new MatrixCode(null)).isInstanceOf(NullPointerException.class);
    }
}
