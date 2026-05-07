package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnalysisKindTest {

    @Test
    void has_three_values_in_declared_order() {
        assertThat(AnalysisKind.values())
                .containsExactly(
                        AnalysisKind.SIMPLE, AnalysisKind.GROUPED, AnalysisKind.SOCIETY_PANEL);
    }
}
