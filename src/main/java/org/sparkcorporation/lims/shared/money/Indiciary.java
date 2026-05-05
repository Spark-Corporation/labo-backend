package org.sparkcorporation.lims.shared.money;

/**
 * "Valeur indiciaire (B)" — the West African medical-nomenclature pricing index (CDC "Boutons de
 * fenêtre en général / Assurances" — valeur indiciaire des analyses). An analysis with {@code B =
 * 5} priced at {@code bRate = 1500 XOF/B} costs {@code 5 × 1500 = 7500 XOF}.
 *
 * <p>The {@code bRate} is configured per lab in catalog data and is not part of {@code shared}.
 */
public record Indiciary(int b) {

    public Indiciary {
        if (b < 0) {
            throw new IllegalArgumentException("b must be ≥ 0, got " + b);
        }
    }

    /** Price = b × bRate. Returns {@link Money#zero(java.util.Currency)} when {@code b == 0}. */
    public Money price(Money bRate) {
        if (b == 0) {
            return Money.zero(bRate.currency());
        }
        return bRate.multiply(b);
    }
}
