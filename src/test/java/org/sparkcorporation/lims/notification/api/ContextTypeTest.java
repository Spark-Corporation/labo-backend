package org.sparkcorporation.lims.notification.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContextTypeTest {

    @Test
    void has_sample_context() {
        assertThat(ContextType.valueOf("SAMPLE")).isEqualTo(ContextType.SAMPLE);
    }
}
