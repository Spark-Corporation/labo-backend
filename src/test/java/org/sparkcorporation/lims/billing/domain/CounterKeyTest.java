package org.sparkcorporation.lims.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CounterKey}'s equals/hashCode contract. Covers the four equals-branches
 * (reference, type, labCode-equal, date-equal) JaCoCo otherwise flags as missed.
 */
class CounterKeyTest {

    private static final LocalDate D1 = LocalDate.of(2026, 5, 13);
    private static final LocalDate D2 = LocalDate.of(2026, 5, 14);

    @Test
    void equals_returns_true_for_same_reference() {
        var key = new CounterKey("LAB", D1);
        assertThat(key).isEqualTo(key);
    }

    @Test
    void equals_returns_true_for_same_labCode_and_date() {
        var a = new CounterKey("LAB", D1);
        var b = new CounterKey("LAB", D1);
        assertThat(a).isEqualTo(b);
        assertThat(b).isEqualTo(a);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void equals_returns_false_for_different_labCode() {
        var a = new CounterKey("LAB", D1);
        var b = new CounterKey("LBX", D1);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returns_false_for_different_date() {
        var a = new CounterKey("LAB", D1);
        var b = new CounterKey("LAB", D2);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_returns_false_for_null_argument() {
        var a = new CounterKey("LAB", D1);
        assertThat(a).isNotEqualTo(null);
    }

    @Test
    void equals_returns_false_for_different_type() {
        var a = new CounterKey("LAB", D1);
        assertThat(a).isNotEqualTo("not a key");
    }

    @Test
    void constructor_rejects_null_labCode() {
        assertThatThrownBy(() -> new CounterKey(null, D1))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("labCode");
    }

    @Test
    void constructor_rejects_null_date() {
        assertThatThrownBy(() -> new CounterKey("LAB", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("date");
    }

    @Test
    void getters_expose_labCode_and_date() {
        var k = new CounterKey("LAB", D1);
        assertThat(k.getLabCode()).isEqualTo("LAB");
        assertThat(k.getDate()).isEqualTo(D1);
    }
}
