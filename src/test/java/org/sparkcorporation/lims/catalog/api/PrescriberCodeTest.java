package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PrescriberCodeTest {

    @Test
    void accepts_auto_generated_format() {
        assertThat(PrescriberCode.of("PRS-000001").value()).isEqualTo("PRS-000001");
        assertThat(PrescriberCode.of("PRS-999999").value()).isEqualTo("PRS-999999");
    }

    @Test
    void rejects_wrong_prefix() {
        assertThatThrownBy(() -> PrescriberCode.of("PHL-000001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_lowercase_prefix() {
        assertThatThrownBy(() -> PrescriberCode.of("prs-000001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_few_digits() {
        assertThatThrownBy(() -> PrescriberCode.of("PRS-12345"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_many_digits() {
        assertThatThrownBy(() -> PrescriberCode.of("PRS-1234567"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_missing_separator() {
        assertThatThrownBy(() -> PrescriberCode.of("PRS000001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> PrescriberCode.of(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> PrescriberCode.of(null)).isInstanceOf(NullPointerException.class);
    }
}
