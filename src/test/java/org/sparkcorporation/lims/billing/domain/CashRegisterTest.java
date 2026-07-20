package org.sparkcorporation.lims.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.iam.api.UserId;

class CashRegisterTest {

    private static final Instant T0 = Instant.parse("2026-05-13T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

    @Test
    void create_sets_code_label_sequence_and_timestamps() {
        var cr = CashRegister.create(new NewCashRegister("C1", "Caisse 1", 1), CLOCK);

        assertThat(cr.getCode()).isEqualTo("C1");
        assertThat(cr.getLabel()).isEqualTo("Caisse 1");
        assertThat(cr.getSequenceNumber()).isEqualTo(1);
        assertThat(cr.getCreatedAt()).isEqualTo(T0);
        assertThat(cr.getUpdatedAt()).isEqualTo(T0);
        assertThat(cr.isDisabled()).isFalse();
        assertThat(cr.userIds()).isEmpty();
    }

    @Test
    void create_rejects_invalid_code_regex() {
        assertThatThrownBy(
                        () -> CashRegister.create(new NewCashRegister("c1", "Caisse 1", 1), CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid cash register code");
    }

    @Test
    void create_rejects_too_long_code() {
        assertThatThrownBy(
                        () -> CashRegister.create(new NewCashRegister("ABC", "Caisse 1", 1), CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_only_mutates_label() {
        var cr = CashRegister.create(new NewCashRegister("C1", "Caisse 1", 1), CLOCK);
        cr.update(new UpdateCashRegister("Caisse Réception"), CLOCK);

        assertThat(cr.getLabel()).isEqualTo("Caisse Réception");
        assertThat(cr.getCode()).isEqualTo("C1");
        assertThat(cr.getSequenceNumber()).isEqualTo(1);
    }

    @Test
    void update_with_null_label_leaves_unchanged() {
        var cr = CashRegister.create(new NewCashRegister("C1", "Caisse 1", 1), CLOCK);
        cr.update(new UpdateCashRegister(null), CLOCK);

        assertThat(cr.getLabel()).isEqualTo("Caisse 1");
    }

    @Test
    void disable_is_idempotent_in_terms_of_disabledAt_marker() {
        var cr = CashRegister.create(new NewCashRegister("C1", "Caisse 1", 1), CLOCK);

        cr.disable(CLOCK);
        assertThat(cr.isDisabled()).isTrue();
        var firstDisabledAt = cr.getDisabledAt();

        // Re-disable with a later clock should NOT overwrite disabledAt — the original disable
        // wins (audit-trail semantics: when was it first disabled, not the last time we attempted).
        var later = Clock.fixed(T0.plusSeconds(60), ZoneOffset.UTC);
        cr.disable(later);

        assertThat(cr.isDisabled()).isTrue();
        assertThat(cr.getDisabledAt()).isEqualTo(firstDisabledAt);
    }

    @Test
    void enable_clears_disabledAt() {
        var cr = CashRegister.create(new NewCashRegister("C1", "Caisse 1", 1), CLOCK);
        cr.disable(CLOCK);
        assertThat(cr.isDisabled()).isTrue();

        cr.enable(CLOCK);
        assertThat(cr.isDisabled()).isFalse();
        assertThat(cr.getDisabledAt()).isNull();
    }

    @Test
    void replaceAssignments_replaces_existing_set() {
        var cr = CashRegister.create(new NewCashRegister("C1", "Caisse 1", 1), CLOCK);
        var userA = UserId.next();
        var userB = UserId.next();
        var userC = UserId.next();

        cr.replaceAssignments(Set.of(userA, userB), CLOCK);
        assertThat(cr.userIds()).containsExactlyInAnyOrder(userA.value(), userB.value());

        // Replace with {B, C} — A must disappear, not be retained as an append.
        cr.replaceAssignments(Set.of(userB, userC), CLOCK);
        assertThat(cr.userIds())
                .containsExactlyInAnyOrder(userB.value(), userC.value())
                .doesNotContain(userA.value());
    }

    @Test
    void replaceAssignments_with_empty_set_clears_all() {
        var cr = CashRegister.create(new NewCashRegister("C1", "Caisse 1", 1), CLOCK);
        cr.replaceAssignments(Set.of(UserId.next(), UserId.next()), CLOCK);
        assertThat(cr.userIds()).hasSize(2);

        cr.replaceAssignments(Set.of(), CLOCK);
        assertThat(cr.userIds()).isEmpty();
    }

    @Test
    void isUserAssigned_reflects_current_set() {
        var cr = CashRegister.create(new NewCashRegister("C1", "Caisse 1", 1), CLOCK);
        var userA = UserId.next();
        var userB = UserId.next();
        cr.replaceAssignments(Set.of(userA), CLOCK);

        assertThat(cr.isUserAssigned(userA.value())).isTrue();
        assertThat(cr.isUserAssigned(userB.value())).isFalse();
    }
}
