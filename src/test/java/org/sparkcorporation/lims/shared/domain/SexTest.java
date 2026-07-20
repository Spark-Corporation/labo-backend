package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SexTest {

    @Test
    void declares_M_and_F_only() {
        assertThat(Sex.values()).containsExactly(Sex.M, Sex.F);
    }

    @Test
    void valueOf_round_trips_M() {
        assertThat(Sex.valueOf("M")).isEqualTo(Sex.M);
    }

    @Test
    void valueOf_round_trips_F() {
        assertThat(Sex.valueOf("F")).isEqualTo(Sex.F);
    }
}
