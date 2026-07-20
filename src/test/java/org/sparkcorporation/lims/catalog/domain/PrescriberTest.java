package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.id.Ulid;

class PrescriberTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);

    private static Specialty newSpecialty(String code) {
        return Specialty.create(new NewSpecialty(code, code + " name", null), FIXED);
    }

    private static NewPrescriber baseCmd() {
        return new NewPrescriber("Dr.", "AGBETI Kossi", null, null, null, null, null);
    }

    @Test
    void create_assigns_ulid_id() {
        var p = Prescriber.create(baseCmd(), "PRS-000001", null, FIXED);
        assertThat(p.id().value()).isNotNull();
        assertThat(Ulid.isValid(p.id().value())).isTrue();
    }

    @Test
    void create_validates_code_via_PrescriberCode() {
        // PrescriberCode regex: "^PRS-\\d{6}$".
        assertThatThrownBy(() -> Prescriber.create(baseCmd(), "BAD-CODE", null, FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid prescriber code");
    }

    @Test
    void create_with_null_specialty_succeeds() {
        var p = Prescriber.create(baseCmd(), "PRS-000001", null, FIXED);
        assertThat(p.specialty()).isNull();
        assertThat(p.code()).isEqualTo("PRS-000001");
        assertThat(p.name()).isEqualTo("AGBETI Kossi");
    }

    @Test
    void create_with_specialty_attaches_it() {
        var sp = newSpecialty("CARDIO");
        var p = Prescriber.create(baseCmd(), "PRS-000001", sp, FIXED);
        assertThat(p.specialty()).isSameAs(sp);
    }

    @Test
    void update_replaces_specialty_when_passed() {
        var sp1 = newSpecialty("CARDIO");
        var sp2 = newSpecialty("DERMATO");
        var p = Prescriber.create(baseCmd(), "PRS-000001", sp1, FIXED);

        p.update(new UpdatePrescriber(null, null, null, null, null, null, null), sp2, FIXED);
        assertThat(p.specialty()).isSameAs(sp2);
    }

    @Test
    void update_with_null_specialty_keeps_existing() {
        var sp = newSpecialty("CARDIO");
        var p = Prescriber.create(baseCmd(), "PRS-000001", sp, FIXED);

        p.update(
                new UpdatePrescriber(null, "AGBETI Komlan", null, null, null, null, null),
                null,
                FIXED);
        assertThat(p.specialty()).isSameAs(sp);
        assertThat(p.name()).isEqualTo("AGBETI Komlan");
    }

    @Test
    void is_active_true_when_not_deleted() {
        var p = Prescriber.create(baseCmd(), "PRS-000001", null, FIXED);
        assertThat(p.isActive()).isTrue();
        assertThat(p.deletedAt()).isNull();
    }
}
