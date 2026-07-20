package org.sparkcorporation.lims.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Currency-aware monetary amount. Scale is normalized to the currency's {@link
 * Currency#getDefaultFractionDigits()} on construction (XOF: 0; USD: 2). All arithmetic methods
 * enforce a single-currency invariant.
 *
 * <p>{@code shared} is Spring-free. Consumers needing a configured default currency wire it in
 * their own {@code @Configuration}; this type does not provide one.
 *
 * <p>Implements {@link Comparable} within a single currency only; {@link #compareTo} throws {@link
 * IllegalArgumentException} on currency mismatch. {@code Money} should not be placed in sorted
 * collections that may contain mixed currencies.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.scale() > currency.getDefaultFractionDigits()) {
            amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        } else if (amount.scale() < currency.getDefaultFractionDigits()) {
            amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.UNNECESSARY);
        }
    }

    public static Money of(long amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    /** Exact multiplication by an integer factor; no rounding. */
    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    /** Multiplication by an arbitrary scalar; caller picks the rounding mode. */
    public Money multiply(BigDecimal scalar, RoundingMode rounding) {
        Objects.requireNonNull(scalar, "scalar");
        Objects.requireNonNull(rounding, "rounding");
        BigDecimal product =
                amount.multiply(scalar).setScale(currency.getDefaultFractionDigits(), rounding);
        return new Money(product, currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "currency mismatch: expected="
                            + currency.getCurrencyCode()
                            + " actual="
                            + other.currency.getCurrencyCode());
        }
    }
}
