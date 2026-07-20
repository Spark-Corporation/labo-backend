package org.sparkcorporation.lims.patient.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PatientMergedTest {

    @Test
    void rejects_null_source() {
        assertThatThrownBy(() -> new PatientMerged(null, PatientId.next(), Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_target() {
        assertThatThrownBy(() -> new PatientMerged(PatientId.next(), null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_mergedAt() {
        assertThatThrownBy(() -> new PatientMerged(PatientId.next(), PatientId.next(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_self_merge() {
        var id = PatientId.next();
        assertThatThrownBy(() -> new PatientMerged(id, id, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itself");
    }

    @Test
    void valid_construction() {
        var src = PatientId.next();
        var tgt = PatientId.next();
        var when = Instant.parse("2026-05-05T10:00:00Z");
        var ev = new PatientMerged(src, tgt, when);
        assertThat(ev.source()).isEqualTo(src);
        assertThat(ev.target()).isEqualTo(tgt);
        assertThat(ev.mergedAt()).isEqualTo(when);
    }
}
