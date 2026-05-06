package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

public record UpdateRecipientRequest(
        @Size(max = 120) @Nullable String name,
        @Nullable String description,
        @Size(max = 30) @Nullable String color,
        @DecimalMin("0.0") @Digits(integer = 4, fraction = 2) @Nullable BigDecimal volumeMl,
        @Nullable String defaultMatrixId) {}
