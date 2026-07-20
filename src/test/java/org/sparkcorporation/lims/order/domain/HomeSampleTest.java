package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class HomeSampleTest {

    @Test
    void create_rejects_null_preleveur_id() {
        assertThatThrownBy(
                        () ->
                                HomeSample.create(
                                        /* analysisId */ "01HVALID",
                                        /* recipientId */ "01HVALID",
                                        /* collectedAt */ Instant.parse("2026-05-07T08:00:00Z"),
                                        /* collectedByUserId */ "01HVALID",
                                        /* addressLine */ "12 rue de Lomé",
                                        /* distanceKm */ 5,
                                        /* windowStart */ Instant.parse("2026-05-07T08:00:00Z"),
                                        /* windowEnd */ Instant.parse("2026-05-07T10:00:00Z"),
                                        /* homePreleveurId */ null))
                .isInstanceOf(HomeSampleMissingPreleveurException.class);
    }

    @Test
    void create_rejects_window_end_equal_to_start() {
        var t = Instant.parse("2026-05-07T08:00:00Z");
        assertThatThrownBy(() -> HomeSample.create("a", "r", t, "u", "addr", 1, t, t, "p"))
                .isInstanceOf(InvalidCollectionWindowException.class);
    }

    @Test
    void create_rejects_window_end_before_start() {
        var t = Instant.parse("2026-05-07T08:00:00Z");
        assertThatThrownBy(
                        () ->
                                HomeSample.create(
                                        "a", "r", t, "u", "addr", 1, t.plusSeconds(60), t, "p"))
                .isInstanceOf(InvalidCollectionWindowException.class);
    }

    @Test
    void create_rejects_negative_distance() {
        var t = Instant.parse("2026-05-07T08:00:00Z");
        assertThatThrownBy(
                        () ->
                                HomeSample.create(
                                        "a", "r", t, "u", "addr", -1, t, t.plusSeconds(60), "p"))
                .isInstanceOf(InvalidDistanceException.class);
    }

    @Test
    void create_succeeds_with_valid_inputs() {
        var t = Instant.parse("2026-05-07T08:00:00Z");
        var sample =
                HomeSample.create(
                        "01HVALID",
                        "01HVALID",
                        t,
                        "01HVALID",
                        "12 rue de Lomé",
                        5,
                        t,
                        t.plusSeconds(7200),
                        "01HVALID");
        org.assertj.core.api.Assertions.assertThat(sample.getHomePreleveurId())
                .isEqualTo("01HVALID");
        org.assertj.core.api.Assertions.assertThat(sample.getDistanceKm()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(sample.getAddressLine())
                .isEqualTo("12 rue de Lomé");
    }
}
