package org.sparkcorporation.lims.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Currency;

import org.junit.jupiter.api.Test;

class IndiciaryTest {

    private static final Currency XOF = Currency.getInstance("XOF");

    @Test
    void rejects_negative_b() {
        assertThatThrownBy(() -> new Indiciary(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allows_zero_b() {
        assertThat(new Indiciary(0).b()).isZero();
    }

    @Test
    void price_for_zero_b_is_zero_money_in_bRate_currency() {
        Money price = new Indiciary(0).price(Money.of(1500L, XOF));
        assertThat(price).isEqualTo(Money.zero(XOF));
    }

    @Test
    void price_multiplies_b_by_bRate() {
        Money price = new Indiciary(5).price(Money.of(1500L, XOF));
        assertThat(price).isEqualTo(Money.of(7500L, XOF));
    }

    @Test
    void price_preserves_bRate_currency() {
        Money price = new Indiciary(2).price(Money.of(2000L, XOF));
        assertThat(price.currency()).isEqualTo(XOF);
    }

    @Test
    void equals_null_is_false() {
        Indiciary i = new Indiciary(5);
        assertThat(i.equals(null)).isFalse();
    }
}
