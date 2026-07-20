package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CivilityTest {

    @Test
    void declares_five_CDC_values_in_order() {
        assertThat(Civility.values())
                .containsExactly(
                        Civility.MONSIEUR,
                        Civility.MADAME,
                        Civility.MADEMOISELLE,
                        Civility.ENFANT,
                        Civility.NOUVEAU_NE);
    }

    @Test
    void every_constant_name_fits_VARCHAR_20_storage() {
        for (Civility c : Civility.values()) {
            assertThat(c.name().length())
                    .as("Civility constant '%s' must fit VARCHAR(20)", c.name())
                    .isLessThanOrEqualTo(20);
        }
    }

    @Test
    void valueOf_round_trips_NOUVEAU_NE() {
        assertThat(Civility.valueOf("NOUVEAU_NE")).isEqualTo(Civility.NOUVEAU_NE);
    }
}
