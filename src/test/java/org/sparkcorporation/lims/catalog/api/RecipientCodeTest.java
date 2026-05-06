package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RecipientCodeTest {

    @Test
    void accepts_uppercase_with_underscores() {
        assertThat(new RecipientCode("EDTA").value()).isEqualTo("EDTA");
        assertThat(new RecipientCode("DRY_TUBE").value()).isEqualTo("DRY_TUBE");
        assertThat(new RecipientCode("AB").value()).isEqualTo("AB");
    }

    @Test
    void rejects_lowercase() {
        assertThatThrownBy(() -> new RecipientCode("edta"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_starting_with_digit() {
        assertThatThrownBy(() -> new RecipientCode("1AB"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> new RecipientCode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new RecipientCode("A" + "_".repeat(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new RecipientCode(null)).isInstanceOf(NullPointerException.class);
    }
}
