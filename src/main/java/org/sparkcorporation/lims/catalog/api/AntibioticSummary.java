package org.sparkcorporation.lims.catalog.api;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record AntibioticSummary(
        AntibioticId id,
        String code,
        String name,
        @Nullable String description,
        @Nullable String family,
        BigDecimal referenceDiameter,
        @Nullable BigDecimal intermediateLower,
        @Nullable BigDecimal intermediateUpper,
        boolean active) {}
