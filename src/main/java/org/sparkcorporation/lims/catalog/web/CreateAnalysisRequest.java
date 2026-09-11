package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;

/**
 * Create-Analysis request DTO per spec §5. {@code code} is admin-typed (regex {@code
 * ^[A-Z][A-Z0-9_]{1,29}$}); FK ids are validated for non-emptiness here and resolved to active
 * aggregates by {@link AnalysisService}.
 */
@Schema(description = "Request to create a new catalog analysis.")
public record CreateAnalysisRequest(
        @Schema(description = "Admin-assigned analysis code; immutable after creation.")
                @NotBlank
                @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$")
                String code,
        @Schema(description = "Display name of the analysis.") @NotBlank @Size(max = 160)
                String name,
        @Schema(
                        description =
                                "Discriminator: SIMPLE (leaf), GROUPED (bundles SIMPLE components)"
                                        + " or SOCIETY_PANEL.")
                @NotNull
                AnalysisKind kind,
        @Schema(
                        description =
                                "Pricing index 'B' (valeur indiciaire); invoice amount = B x the"
                                        + " payer's bRate (XOF per point).")
                @NotNull
                @DecimalMin("0.0")
                BigDecimal indiciaryValueB,
        @Schema(description = "Number of result labels to print; defaults to 1 when omitted.")
                @Min(0)
                @Nullable Integer labelCount,
        @Schema(description = "ULID of the discipline this analysis belongs to.")
                @NotBlank
                @Size(max = 26)
                String disciplineId,
        @Schema(
                        description =
                                "ULID of the sample matrix this analysis is performed on, if"
                                        + " applicable.")
                @Size(max = 26)
                @Nullable String matrixId,
        @Schema(
                        description =
                                "ULID of the default sample recipient/container for this analysis,"
                                        + " if applicable.")
                @Size(max = 26)
                @Nullable String recipientId,
        @Schema(
                        description =
                                "How the result is rendered/validated: FORMULA, INTEGER, REAL, TEXT"
                                        + " or FREE ('Autre').")
                @NotNull
                ResultType resultType,
        @Schema(description = "Free-text clinical interpretation text shown alongside the result.")
                @Nullable String interpretationText,
        @Schema(
                        description =
                                "Formula expression used to compute the result (only relevant when"
                                        + " resultType is FORMULA).")
                @Nullable String formulaExpression,
        @Schema(
                        description =
                                "Default sample rack/carrier (portoir) identifier assigned at"
                                        + " collection.")
                @Size(max = 20)
                @Nullable String defaultPortoir,
        @Schema(
                        description =
                                "Default number of days a sample is conserved before its"
                                        + " destruction is due.")
                @Nullable Integer defaultConservationDays,
        @Schema(description = "Result unit configuration.") @Valid @Nullable UnitsRequest units,
        @Schema(description = "Expected result turnaround delay.") @Valid
                @Nullable AnalyticalDelayRequest delay) {}
