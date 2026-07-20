package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record UpdateAntibiotic(
        @Nullable String name,
        @Nullable String description,
        @Nullable String family,
        @Nullable BigDecimal referenceDiameter,
        @Nullable BigDecimal intermediateLower,
        @Nullable BigDecimal intermediateUpper) {}
