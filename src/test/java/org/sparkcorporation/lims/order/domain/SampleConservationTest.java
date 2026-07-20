package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class SampleConservationTest {

    private static final Instant COLLECTED = Instant.parse("2026-06-01T08:00:00Z");
    private static final String ANALYSIS = "01HZB0000000000000000ANA01";
    private static final String RECIPIENT = "01HZB0000000000000000REC01";
    private static final String COLLECTOR = "01HZB0000000000000000STF01";

    private InLabSample collectedSample(Instant collectedAt) {
        // InLabSample.create(analysisId, recipientId, collectedAt, collectedByUserId)
        return InLabSample.create(ANALYSIS, RECIPIENT, collectedAt, COLLECTOR);
    }

    @Test
    void snapshot_computes_due_date_as_start_plus_days() {
        var s = collectedSample(COLLECTED);
        s.snapshotConservation("P12", 30);
        assertThat(s.getPortoir()).isEqualTo("P12");
        assertThat(s.getConservationDays()).isEqualTo(30);
        assertThat(s.getConservationStart()).isEqualTo(COLLECTED);
        assertThat(s.getDestructionDueAt()).isEqualTo(COLLECTED.plus(30, ChronoUnit.DAYS));
    }

    @Test
    void snapshot_with_null_days_leaves_due_date_null() {
        var s = collectedSample(COLLECTED);
        s.snapshotConservation("P12", null);
        assertThat(s.getConservationStart()).isEqualTo(COLLECTED);
        assertThat(s.getDestructionDueAt()).isNull();
    }

    // NOTE: the null-collectedAt branch in snapshotConservation is defensive only —
    // InLabSample.create
    // requires a non-null collectedAt, so a created sample can never reach it. We keep the guard
    // for
    // safety but do not unit-test an unreachable path (scope discipline).

    @Test
    void mark_destroyed_sets_destroyed_fields() {
        var s = collectedSample(COLLECTED);
        var when = Instant.parse("2026-07-15T10:00:00Z");
        s.markDestroyed(when, "01HZB0000000000000000STF09");
        assertThat(s.getDestroyedAt()).isEqualTo(when);
        assertThat(s.getDestroyedByUserId()).isEqualTo("01HZB0000000000000000STF09");
    }
}
