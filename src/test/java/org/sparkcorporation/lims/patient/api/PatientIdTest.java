package org.sparkcorporation.lims.patient.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class PatientIdTest {

    @Test
    void rejects_null_value() {
        assertThatThrownBy(() -> new PatientId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_invalid_ulid() {
        assertThatThrownBy(() -> new PatientId("not-a-ulid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid ULID");
    }

    @Test
    void accepts_valid_ulid() {
        String id = Ulid.generate();
        assertThat(new PatientId(id).value()).isEqualTo(id);
    }

    @Test
    void next_returns_a_new_unique_id() {
        PatientId a = PatientId.next();
        PatientId b = PatientId.next();
        assertThat(a.value()).hasSize(26);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_null_is_false() {
        PatientId p = PatientId.next();
        assertThat(p.equals(null)).isFalse();
    }
}
