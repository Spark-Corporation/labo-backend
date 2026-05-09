package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AnalysisRequestTest {

    @Test
    void create_populates_id_and_snapshot_code() {
        var requestedAt = Instant.parse("2026-05-07T08:00:00Z");
        var ar = AnalysisRequest.create("01ANALYSIS", "GLUC", requestedAt);

        assertThat(ar.getAnalysisId()).isEqualTo("01ANALYSIS");
        assertThat(ar.getAnalysisCode()).isEqualTo("GLUC");
        assertThat(ar.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(ar.rawId()).isNotNull().hasSize(26);
    }

    @Test
    void create_rejects_null_analysis_id() {
        var t = Instant.parse("2026-05-07T08:00:00Z");
        assertThatThrownBy(() -> AnalysisRequest.create(null, "GLUC", t))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejects_null_analysis_code() {
        var t = Instant.parse("2026-05-07T08:00:00Z");
        assertThatThrownBy(() -> AnalysisRequest.create("01ANALYSIS", null, t))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_rejects_null_requested_at() {
        assertThatThrownBy(() -> AnalysisRequest.create("01ANALYSIS", "GLUC", null))
                .isInstanceOf(NullPointerException.class);
    }
}
