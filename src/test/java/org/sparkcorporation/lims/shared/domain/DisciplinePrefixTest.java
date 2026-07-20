package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class DisciplinePrefixTest {

    @Test
    void declares_five_CDC_disciplines_in_order() {
        assertThat(DisciplinePrefix.values())
                .containsExactly(
                        DisciplinePrefix.BIOCHIMIE,
                        DisciplinePrefix.HEMATOLOGIE,
                        DisciplinePrefix.IMMUNO_SEROLOGIE,
                        DisciplinePrefix.MICROBIOLOGIE,
                        DisciplinePrefix.BIOLOGIE_MOLECULAIRE);
    }

    @Test
    void prefix_codes_are_1_through_5_unique() {
        assertThat(
                        Arrays.stream(DisciplinePrefix.values())
                                .map(DisciplinePrefix::prefix)
                                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
    }

    @Test
    void byPrefix_returns_BIOCHIMIE_for_1() {
        assertThat(DisciplinePrefix.byPrefix(1)).isEqualTo(DisciplinePrefix.BIOCHIMIE);
    }

    @Test
    void byPrefix_returns_BIOLOGIE_MOLECULAIRE_for_5() {
        assertThat(DisciplinePrefix.byPrefix(5)).isEqualTo(DisciplinePrefix.BIOLOGIE_MOLECULAIRE);
    }

    @Test
    void byPrefix_throws_IAE_on_zero() {
        assertThatThrownBy(() -> DisciplinePrefix.byPrefix(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0");
    }

    @Test
    void byPrefix_throws_IAE_on_six() {
        assertThatThrownBy(() -> DisciplinePrefix.byPrefix(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6");
    }

    @Test
    void displayName_uses_French_label() {
        assertThat(DisciplinePrefix.IMMUNO_SEROLOGIE.displayName()).isEqualTo("Immuno-sérologie");
    }
}
