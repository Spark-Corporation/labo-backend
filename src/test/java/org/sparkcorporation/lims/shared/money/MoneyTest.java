package org.sparkcorporation.lims.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency XOF = Currency.getInstance("XOF");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    @DisplayName("XOF is a known JDK currency with zero default fraction digits")
    void xof_is_known_zero_scale_currency() {
        assertThat(XOF).isNotNull();
        assertThat(XOF.getDefaultFractionDigits()).isZero();
    }

    @Test
    void rejects_null_amount() {
        assertThatThrownBy(() -> new Money(null, XOF)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_currency() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void normalizes_scale_to_currency_default_for_XOF() {
        Money m = new Money(new BigDecimal("1500.50"), XOF);
        assertThat(m.amount().scale()).isZero();
        assertThat(m.amount()).isEqualTo(new BigDecimal("1501"));
    }

    @Test
    void normalizes_scale_to_currency_default_for_USD() {
        Money m = new Money(new BigDecimal("12.345"), USD);
        assertThat(m.amount().scale()).isEqualTo(2);
        assertThat(m.amount()).isEqualTo(new BigDecimal("12.35"));
    }

    @Test
    void preserves_amount_when_already_at_default_scale() {
        Money m = new Money(new BigDecimal("100"), XOF);
        assertThat(m.amount()).isEqualTo(new BigDecimal("100"));
        assertThat(m.amount().scale()).isZero();
    }

    @Test
    void of_long_constructs_zero_scale_amount() {
        Money m = Money.of(500L, XOF);
        assertThat(m.amount()).isEqualTo(new BigDecimal("500"));
        assertThat(m.currency()).isEqualTo(XOF);
    }

    @Test
    void zero_returns_zero_amount_at_currency_scale() {
        Money z = Money.zero(USD);
        assertThat(z.amount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(z.amount().scale()).isEqualTo(2);
    }

    @Test
    void add_combines_same_currency() {
        Money a = Money.of(1000L, XOF);
        Money b = Money.of(500L, XOF);
        assertThat(a.add(b)).isEqualTo(Money.of(1500L, XOF));
    }

    @Test
    void add_throws_on_currency_mismatch() {
        Money a = Money.of(1000L, XOF);
        Money b = new Money(new BigDecimal("10.00"), USD);
        assertThatThrownBy(() -> a.add(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void subtract_combines_same_currency() {
        assertThat(Money.of(1000L, XOF).subtract(Money.of(300L, XOF)))
                .isEqualTo(Money.of(700L, XOF));
    }

    @Test
    void subtract_throws_on_currency_mismatch() {
        Money a = Money.of(1000L, XOF);
        Money b = new Money(new BigDecimal("10.00"), USD);
        assertThatThrownBy(() -> a.subtract(b)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negate_flips_sign_preserves_currency() {
        Money n = Money.of(500L, XOF).negate();
        assertThat(n.amount()).isEqualTo(new BigDecimal("-500"));
        assertThat(n.currency()).isEqualTo(XOF);
    }

    @Test
    void multiply_int_is_exact() {
        assertThat(Money.of(500L, XOF).multiply(3)).isEqualTo(Money.of(1500L, XOF));
    }

    @Test
    void multiply_int_zero_returns_zero() {
        assertThat(Money.of(500L, XOF).multiply(0)).isEqualTo(Money.zero(XOF));
    }

    @Test
    void multiply_int_negative_flips_sign() {
        assertThat(Money.of(500L, XOF).multiply(-2)).isEqualTo(Money.of(-1000L, XOF));
    }

    @Test
    void multiply_BigDecimal_with_HALF_UP_rounds_correctly_for_XOF() {
        Money m = Money.of(1000L, XOF).multiply(new BigDecimal("0.075"), RoundingMode.HALF_UP);
        assertThat(m).isEqualTo(Money.of(75L, XOF));
    }

    @Test
    void multiply_BigDecimal_with_HALF_UP_rounds_correctly_for_USD() {
        Money m =
                new Money(new BigDecimal("100.00"), USD)
                        .multiply(new BigDecimal("0.075"), RoundingMode.HALF_UP);
        assertThat(m.amount()).isEqualTo(new BigDecimal("7.50"));
    }

    @Test
    void multiply_BigDecimal_with_HALF_DOWN_truncates_at_half() {
        Money m =
                new Money(new BigDecimal("100.00"), USD)
                        .multiply(new BigDecimal("0.005"), RoundingMode.HALF_DOWN);
        assertThat(m.amount()).isEqualTo(new BigDecimal("0.50"));
    }

    @Test
    void isZero_isPositive_isNegative_agree_with_signum() {
        assertThat(Money.zero(XOF).isZero()).isTrue();
        assertThat(Money.of(1L, XOF).isPositive()).isTrue();
        assertThat(Money.of(-1L, XOF).isNegative()).isTrue();
        assertThat(Money.zero(XOF).isPositive()).isFalse();
        assertThat(Money.zero(XOF).isNegative()).isFalse();
    }

    @Test
    void compareTo_throws_on_currency_mismatch() {
        Money a = Money.of(100L, XOF);
        Money b = new Money(new BigDecimal("100.00"), USD);
        assertThatThrownBy(() -> a.compareTo(b)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compareTo_antisymmetry() {
        Money a = Money.of(100L, XOF);
        Money b = Money.of(200L, XOF);
        assertThat(Integer.signum(a.compareTo(b))).isEqualTo(-Integer.signum(b.compareTo(a)));
    }

    @Test
    void compareTo_transitivity() {
        Money a = Money.of(100L, XOF);
        Money b = Money.of(200L, XOF);
        Money c = Money.of(300L, XOF);
        assertThat(a.compareTo(b)).isNegative();
        assertThat(b.compareTo(c)).isNegative();
        assertThat(a.compareTo(c)).isNegative();
    }

    @Test
    void compareTo_consistent_with_equals() {
        Money a = Money.of(100L, XOF);
        Money b = Money.of(100L, XOF);
        assertThat(a.compareTo(b)).isZero();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void toString_for_XOF_has_no_decimal_point() {
        assertThat(Money.of(1500L, XOF).toString()).isEqualTo("1500 XOF");
    }

    @Test
    void toString_for_USD_keeps_two_decimals() {
        assertThat(new Money(new BigDecimal("12.50"), USD).toString()).isEqualTo("12.50 USD");
    }

    @Test
    void toString_negative_uses_plain_minus() {
        assertThat(Money.of(-500L, XOF).toString()).isEqualTo("-500 XOF");
    }

    @Test
    void toString_zero() {
        assertThat(Money.zero(XOF).toString()).isEqualTo("0 XOF");
        assertThat(Money.zero(USD).toString()).isEqualTo("0.00 USD");
    }

    @Test
    void equals_null_is_false() {
        Money m = Money.of(100L, XOF);
        assertThat(m.equals(null)).isFalse();
    }

    @Test
    void record_equality_includes_currency() {
        Money xofHundred = Money.of(100L, XOF);
        Money usdHundred = new Money(new BigDecimal("100.00"), USD);
        assertThat(xofHundred).isNotEqualTo(usdHundred);
    }
}
