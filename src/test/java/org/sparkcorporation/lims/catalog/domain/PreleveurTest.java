package org.sparkcorporation.lims.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.Habilitation;
import org.sparkcorporation.lims.shared.id.Ulid;

class PreleveurTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void create_rejects_empty_habilitations() {
        var cmd = new NewPreleveur("DJOSSE Ama", null, null, null, Set.<Habilitation>of());
        assertThatThrownBy(() -> Preleveur.create(cmd, "PHL-000001", Clock.systemUTC()))
                .isInstanceOf(MissingHabilitationException.class)
                .hasMessageContaining("habilitation");
    }

    @Test
    void create_with_one_habilitation_succeeds() {
        var cmd =
                new NewPreleveur(
                        "DJOSSE Ama",
                        null,
                        null,
                        null,
                        Set.of(Habilitation.ADULTES_VEINES_VISIBLES));
        var p = Preleveur.create(cmd, "PHL-000001", FIXED);
        assertThat(p.habilitations()).containsExactly(Habilitation.ADULTES_VEINES_VISIBLES);
        assertThat(p.code()).isEqualTo("PHL-000001");
        assertThat(p.name()).isEqualTo("DJOSSE Ama");
    }

    @Test
    void update_with_empty_habilitations_throws() {
        var p =
                Preleveur.create(
                        new NewPreleveur(
                                "DJOSSE Ama",
                                null,
                                null,
                                null,
                                Set.of(Habilitation.ADULTES_VEINES_VISIBLES)),
                        "PHL-000001",
                        FIXED);
        var cmd = new UpdatePreleveur(null, null, null, null, Set.<Habilitation>of());
        assertThatThrownBy(() -> p.update(cmd, FIXED))
                .isInstanceOf(MissingHabilitationException.class);
    }

    @Test
    void update_with_null_habilitations_keeps_existing() {
        var initial =
                Set.of(Habilitation.ADULTES_VEINES_VISIBLES, Habilitation.ENFANTS_VEINES_FINES);
        var p =
                Preleveur.create(
                        new NewPreleveur("DJOSSE Ama", null, null, null, initial),
                        "PHL-000001",
                        FIXED);

        p.update(new UpdatePreleveur("DJOSSE Amélie", null, null, null, null), FIXED);

        assertThat(p.habilitations()).containsExactlyInAnyOrderElementsOf(initial);
        assertThat(p.name()).isEqualTo("DJOSSE Amélie");
    }

    @Test
    void create_assigns_ulid_id() {
        var p =
                Preleveur.create(
                        new NewPreleveur(
                                "DJOSSE Ama", null, null, null, Set.of(Habilitation.TOUT_LE_MONDE)),
                        "PHL-000001",
                        FIXED);
        assertThat(p.id().value()).isNotNull();
        assertThat(Ulid.isValid(p.id().value())).isTrue();
    }

    @Test
    void create_with_invalid_code_format_throws() {
        var cmd =
                new NewPreleveur(
                        "DJOSSE Ama", null, null, null, Set.of(Habilitation.TOUT_LE_MONDE));
        // PreleveurCode regex: "^PHL-\\d{6}$"; missing prefix and digit-count.
        assertThatThrownBy(() -> Preleveur.create(cmd, "PRS-000001", FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid preleveur code");
    }

    @Test
    void is_active_true_when_not_deleted() {
        var p =
                Preleveur.create(
                        new NewPreleveur(
                                "DJOSSE Ama", null, null, null, Set.of(Habilitation.TOUT_LE_MONDE)),
                        "PHL-000001",
                        FIXED);
        assertThat(p.isActive()).isTrue();
        assertThat(p.deletedAt()).isNull();
    }
}
