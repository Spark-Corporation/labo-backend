package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitivityTest {

    @Test
    void declares_R_I_S_in_order() {
        assertThat(Sensitivity.values())
                .containsExactly(Sensitivity.R, Sensitivity.I, Sensitivity.S);
    }

    @Test
    void valueOf_round_trips_each_value() {
        assertThat(Sensitivity.valueOf("R")).isEqualTo(Sensitivity.R);
        assertThat(Sensitivity.valueOf("I")).isEqualTo(Sensitivity.I);
        assertThat(Sensitivity.valueOf("S")).isEqualTo(Sensitivity.S);
    }
}
