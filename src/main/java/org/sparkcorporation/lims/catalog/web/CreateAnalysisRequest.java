package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;

/**
 * Create-Analysis request DTO per spec §5. {@code code} is admin-typed (regex {@code
 * ^[A-Z][A-Z0-9_]{1,29}$}); FK ids are validated for non-emptiness here and resolved to active
 * aggregates by {@link AnalysisService}.
 */
public record CreateAnalysisRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$") String code,
        @NotBlank @Size(max = 160) String name,
        @NotNull AnalysisKind kind,
        @NotNull @DecimalMin("0.0") BigDecimal indiciaryValueB,
        @Min(0) @Nullable Integer labelCount,
        @NotBlank @Size(max = 26) String disciplineId,
        @Size(max = 26) @Nullable String matrixId,
        @Size(max = 26) @Nullable String recipientId,
        @NotNull ResultType resultType,
        @Nullable String interpretationText,
        @Nullable String formulaExpression,
        @Size(max = 20) @Nullable String defaultPortoir,
        @Nullable Integer defaultConservationDays,
        @Valid @Nullable UnitsRequest units,
        @Valid @Nullable AnalyticalDelayRequest delay) {}
