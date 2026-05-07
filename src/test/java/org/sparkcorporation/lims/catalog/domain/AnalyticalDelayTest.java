package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnalyticalDelayTest {

    @Test
    void empty_returns_all_null() {
        var d = AnalyticalDelay.empty();
        assertThat(d.days()).isNull();
        assertThat(d.hours()).isNull();
        assertThat(d.anteriorityCount()).isNull();
    }

    @Test
    void accepts_arbitrary_positive_values() {
        var d = new AnalyticalDelay(2, 4, 5);
        assertThat(d.days()).isEqualTo(2);
        assertThat(d.hours()).isEqualTo(4);
        assertThat(d.anteriorityCount()).isEqualTo(5);
    }
}
