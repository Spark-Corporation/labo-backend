package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

public record CreateRecipientRequest(
        @NotBlank @Size(max = 30) @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$") String code,
        @NotBlank @Size(max = 120) String name,
        @Nullable String description,
        @Size(max = 30) @Nullable String color,
        @DecimalMin("0.0") @Digits(integer = 4, fraction = 2) @Nullable BigDecimal volumeMl,
        @Nullable String defaultMatrixId) {}
