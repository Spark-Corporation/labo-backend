package org.sparkcorporation.lims.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgeUnitTest {

    @Test
    void declares_DAYS_MONTHS_YEARS_in_order() {
        assertThat(AgeUnit.values()).containsExactly(AgeUnit.DAYS, AgeUnit.MONTHS, AgeUnit.YEARS);
    }
}
