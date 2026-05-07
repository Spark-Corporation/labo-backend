package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class SpecialtyTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void create_assigns_ulid_id() {
        var s = Specialty.create(new NewSpecialty("CARDIOLOGY", "Cardiologie", null), FIXED);
        assertThat(s.id().value()).isNotNull();
        assertThat(Ulid.isValid(s.id().value())).isTrue();
    }

    @Test
    void create_validates_code_via_SpecialtyCode_of() {
        // SpecialtyCode rejects lowercase: regex requires "^[A-Z][A-Z0-9_]{1,29}$".
        assertThatThrownBy(
                        () ->
                                Specialty.create(
                                        new NewSpecialty("cardiology", "Cardiologie", null), FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid specialty code");
    }

    @Test
    void update_changes_name_and_description() {
        var s = Specialty.create(new NewSpecialty("CARDIO", "Cardiologie", "old"), FIXED);
        s.updateNameAndDescription(new UpdateSpecialty("Cardiologie clinique", "new desc"), FIXED);
        assertThat(s.name()).isEqualTo("Cardiologie clinique");
        assertThat(s.description()).isEqualTo("new desc");
    }

    @Test
    void update_with_null_skips_field() {
        var s = Specialty.create(new NewSpecialty("CARDIO", "Cardiologie", "desc"), FIXED);
        s.updateNameAndDescription(new UpdateSpecialty(null, null), FIXED);
        assertThat(s.name()).isEqualTo("Cardiologie");
        assertThat(s.description()).isEqualTo("desc");
    }

    @Test
    void is_active_true_when_not_deleted() {
        var s = Specialty.create(new NewSpecialty("CARDIO", "Cardiologie", null), FIXED);
        assertThat(s.isActive()).isTrue();
        assertThat(s.deletedAt()).isNull();
    }
}
