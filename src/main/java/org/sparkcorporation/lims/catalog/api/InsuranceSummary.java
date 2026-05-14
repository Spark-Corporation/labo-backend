package org.sparkcorporation.lims.catalog.api;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/** Cross-module read shape for {@code Insurance} (billing spec §4.2). */
public record InsuranceSummary(
        InsuranceId id,
        String code,
        String name,
        @Nullable String responsibleEmail,
        @Nullable String responsibleContact,
        BigDecimal bRate,
        BigDecimal coverageRate,
        boolean disabled) {}
