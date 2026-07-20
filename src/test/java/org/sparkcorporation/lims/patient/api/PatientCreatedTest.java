package org.sparkcorporation.lims.patient.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PatientCreatedTest {

    @Test
    void rejects_null_id() {
        assertThatThrownBy(() -> new PatientCreated(null, "LAB-260505-001", Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_labCode() {
        assertThatThrownBy(() -> new PatientCreated(PatientId.next(), null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_createdAt() {
        assertThatThrownBy(() -> new PatientCreated(PatientId.next(), "LAB-260505-001", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void valid_construction() {
        var id = PatientId.next();
        var when = Instant.parse("2026-05-05T10:00:00Z");
        var ev = new PatientCreated(id, "LAB-260505-001", when);
        assertThat(ev.id()).isEqualTo(id);
        assertThat(ev.labCode()).isEqualTo("LAB-260505-001");
        assertThat(ev.createdAt()).isEqualTo(when);
    }
}
