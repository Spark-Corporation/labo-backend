package org.sparkcorporation.lims.catalog.api;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/** Cross-module read shape for {@code Partner} (billing spec §4.3). No coverage rate. */
public record PartnerSummary(
        PartnerId id,
        String code,
        String name,
        PartnerKind kind,
        @Nullable String responsibleEmail,
        @Nullable String responsibleContact,
        BigDecimal bRate,
        boolean disabled) {}
