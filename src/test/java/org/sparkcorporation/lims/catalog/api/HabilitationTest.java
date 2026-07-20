package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HabilitationTest {

    @Test
    void declares_seven_values_in_cdc_order() {
        // CDC §5 verbatim list — order preserved for i18n key stability
        assertThat(Habilitation.values())
                .containsExactly(
                        Habilitation.TOUT_LE_MONDE,
                        Habilitation.ADULTES_VEINES_VISIBLES,
                        Habilitation.ADULTES_VEINES_PROFONDES_OU_FINES,
                        Habilitation.PERSONNES_EN_FORME,
                        Habilitation.ENFANTS_VEINES_VISIBLES,
                        Habilitation.ENFANTS_VEINES_FINES,
                        Habilitation.NOURRISSON_NOUVEAU_NE);
    }
}
