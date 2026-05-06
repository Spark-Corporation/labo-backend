package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record AntibioticResponse(
        String id,
        String code,
        String name,
        @Nullable String description,
        @Nullable String family,
        BigDecimal referenceDiameter,
        @Nullable BigDecimal intermediateLower,
        @Nullable BigDecimal intermediateUpper,
        @Nullable Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        int version) {}
