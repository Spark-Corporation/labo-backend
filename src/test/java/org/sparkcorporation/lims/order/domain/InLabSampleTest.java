package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class InLabSampleTest {

    @Test
    void create_succeeds_with_required_fields() {
        var t = Instant.parse("2026-05-07T08:00:00Z");
        var sample = InLabSample.create("01ANALYSIS", "01RECIPIENT", t, "01USER");

        assertThat(sample.getAnalysisId()).isEqualTo("01ANALYSIS");
        assertThat(sample.getRecipientId()).isEqualTo("01RECIPIENT");
        assertThat(sample.getCollectedAt()).isEqualTo(t);
        assertThat(sample.getCollectedByUserId()).isEqualTo("01USER");
        assertThat(sample.rawId()).isNotNull().hasSize(26);
    }

    @Test
    void create_rejects_null_collected_at() {
        assertThatThrownBy(() -> InLabSample.create("a", "r", null, "u"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("collectedAt");
    }

    @Test
    void create_rejects_null_analysis_id() {
        var t = Instant.parse("2026-05-07T08:00:00Z");
        assertThatThrownBy(() -> InLabSample.create(null, "r", t, "u"))
                .isInstanceOf(NullPointerException.class);
    }
}
