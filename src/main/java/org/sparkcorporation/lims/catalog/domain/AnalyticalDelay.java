package org.sparkcorporation.lims.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

/**
 * Embedded value object on the {@code Analysis} aggregate per CDC §8 onglet "Délai analytique".
 *
 * <p>Mapped to columns {@code delay_days, delay_hours, anteriority_count}. All fields nullable; no
 * validation per spec §4.6 (CDC does not bound the values).
 */
@Embeddable
public record AnalyticalDelay(
        @Column(name = "delay_days") @Nullable Integer days,
        @Column(name = "delay_hours") @Nullable Integer hours,
        @Column(name = "anteriority_count") @Nullable Integer anteriorityCount) {

    /** Empty AnalyticalDelay (all fields null). */
    public static AnalyticalDelay empty() {
        return new AnalyticalDelay(null, null, null);
    }
}
