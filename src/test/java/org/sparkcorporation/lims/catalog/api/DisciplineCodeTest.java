package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisciplineCodeTest {

    @Test
    void accepts_uppercase_with_underscores() {
        assertThat(new DisciplineCode("BIOCHIMIE").value()).isEqualTo("BIOCHIMIE");
        assertThat(new DisciplineCode("IMMUNO_SEROLOGIE").value()).isEqualTo("IMMUNO_SEROLOGIE");
        assertThat(new DisciplineCode("AB").value()).isEqualTo("AB");
    }

    @Test
    void rejects_lowercase() {
        assertThatThrownBy(() -> new DisciplineCode("biochimie"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_starting_with_digit() {
        assertThatThrownBy(() -> new DisciplineCode("1AB"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> new DisciplineCode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_long() {
        // 31 chars: "A" + 30 underscores
        assertThatThrownBy(() -> new DisciplineCode("A" + "_".repeat(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new DisciplineCode(null)).isInstanceOf(NullPointerException.class);
    }
}
