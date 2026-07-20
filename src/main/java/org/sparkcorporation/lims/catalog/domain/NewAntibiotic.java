package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record NewAntibiotic(
        String code,
        String name,
        @Nullable String description,
        @Nullable String family,
        BigDecimal referenceDiameter,
        @Nullable BigDecimal intermediateLower,
        @Nullable BigDecimal intermediateUpper) {}
