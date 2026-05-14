package org.sparkcorporation.lims.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.domain.CashRegisterAssignment.CashRegisterAssignmentKey;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Unit tests for {@link CashRegisterAssignmentKey}'s equals/hashCode contract. Covers the four
 * equals-branches JaCoCo otherwise flags as missed (the embedded-key isn't exercised by the JPA
 * tests which instead route through {@link CashRegister}).
 */
class CashRegisterAssignmentKeyTest {

    private final String crId = Ulid.generate();
    private final String userId = Ulid.generate();

    @Test
    void equals_returns_true_for_same_reference() {
        var k = new CashRegisterAssignmentKey(crId, userId);
        assertThat(k).isEqualTo(k);
    }

    @Test
    void equals_returns_true_for_same_components() {
        var a = new CashRegisterAssignmentKey(crId, userId);
        var b = new CashRegisterAssignmentKey(crId, userId);
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void equals_returns_false_for_different_cashRegisterId() {
        var a = new CashRegisterAssignmentKey(crId, userId);
        var b = new CashRegisterAssignmentKey(Ulid.generate(), userId);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returns_false_for_different_userId() {
        var a = new CashRegisterAssignmentKey(crId, userId);
        var b = new CashRegisterAssignmentKey(crId, Ulid.generate());
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returns_false_for_null_argument() {
        var k = new CashRegisterAssignmentKey(crId, userId);
        assertThat(k).isNotEqualTo(null);
    }

    @Test
    void equals_returns_false_for_different_type() {
        var k = new CashRegisterAssignmentKey(crId, userId);
        assertThat(k).isNotEqualTo("not a key");
    }

    @Test
    void constructor_rejects_null_cashRegisterId() {
        assertThatThrownBy(() -> new CashRegisterAssignmentKey(null, userId))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cashRegisterId");
    }

    @Test
    void constructor_rejects_null_userId() {
        assertThatThrownBy(() -> new CashRegisterAssignmentKey(crId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void accessors_expose_both_components() {
        var k = new CashRegisterAssignmentKey(crId, userId);
        assertThat(k.cashRegisterId()).isEqualTo(crId);
        assertThat(k.userId()).isEqualTo(userId);
    }
}
