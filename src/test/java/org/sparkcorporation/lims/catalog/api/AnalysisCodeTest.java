package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AnalysisCodeTest {

    @Test
    void accepts_admin_typed_uppercase_alphanumeric() {
        assertThat(AnalysisCode.of("GLY").value()).isEqualTo("GLY");
        assertThat(AnalysisCode.of("BILAN_GLU_2").value()).isEqualTo("BILAN_GLU_2");
    }

    @Test
    void rejects_lowercase() {
        assertThatThrownBy(() -> AnalysisCode.of("gly"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_starting_digit() {
        assertThatThrownBy(() -> AnalysisCode.of("1GLY"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> AnalysisCode.of("A".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> AnalysisCode.of("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> AnalysisCode.of(null)).isInstanceOf(NullPointerException.class);
    }
}
