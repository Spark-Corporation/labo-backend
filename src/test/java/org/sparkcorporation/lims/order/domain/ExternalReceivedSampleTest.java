package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ExternalReceivedSampleTest {

    @Test
    void create_succeeds_with_collected_at_null() {
        var receivedAt = Instant.parse("2026-05-07T09:00:00Z");
        var sample =
                ExternalReceivedSample.create(
                        "01ANALYSIS",
                        "01RECIPIENT",
                        /* collectedAt */ null,
                        receivedAt,
                        "01USER",
                        "Hospital X");

        assertThat(sample.getCollectedAt()).isNull();
        assertThat(sample.getReceivedAt()).isEqualTo(receivedAt);
        assertThat(sample.getOrigin()).isEqualTo("Hospital X");
        assertThat(sample.getReceivedByUserId()).isEqualTo("01USER");
    }

    @Test
    void create_rejects_null_received_at() {
        assertThatThrownBy(
                        () ->
                                ExternalReceivedSample.create(
                                        "a", "r", null, /* receivedAt */ null, "u", "origin"))
                .isInstanceOf(InvalidReceivedAtException.class);
    }

    @Test
    void create_rejects_null_origin() {
        var receivedAt = Instant.parse("2026-05-07T09:00:00Z");
        assertThatThrownBy(
                        () ->
                                ExternalReceivedSample.create(
                                        "a", "r", null, receivedAt, "u", /* origin */ null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("origin");
    }
}
