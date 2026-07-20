package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

public record UpdateAntibioticRequest(
        @Size(max = 120) @Nullable String name,
        @Nullable String description,
        @Size(max = 80) @Nullable String family,
        @DecimalMin("0.0") @Digits(integer = 3, fraction = 2)
                @Nullable BigDecimal referenceDiameter,
        @DecimalMin("0.0") @Digits(integer = 3, fraction = 2)
                @Nullable BigDecimal intermediateLower,
        @DecimalMin("0.0") @Digits(integer = 3, fraction = 2)
                @Nullable BigDecimal intermediateUpper) {}
