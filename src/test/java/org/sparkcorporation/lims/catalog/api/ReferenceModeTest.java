package org.sparkcorporation.lims.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReferenceModeTest {

    @Test
    void has_three_values_in_declared_order() {
        assertThat(ReferenceMode.values())
                .containsExactly(ReferenceMode.STANDARD, ReferenceMode.COMMENT, ReferenceMode.NONE);
    }
}
