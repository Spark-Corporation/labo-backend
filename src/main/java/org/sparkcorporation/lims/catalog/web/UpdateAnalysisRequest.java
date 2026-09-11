package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ResultType;

/**
 * PATCH semantics: all fields nullable; null = no change. {@code code} and {@code kind} are
 * intentionally absent — both are immutable per spec §5.5; use the {@code :convert-to-grouped} /
 * {@code :convert-to-simple} endpoints to change kind. The controller binds the body as a {@code
 * Map} first to detect and reject those fields explicitly with 422, then delegates here.
 */
@Schema(
        description =
                "Request to partially update an analysis; null fields are left unchanged. 'code'"
                        + " and 'kind' are immutable and not part of this shape.")
public record UpdateAnalysisRequest(
        @Schema(description = "Display name of the analysis.") @Size(max = 160)
                @Nullable String name,
        @Schema(
                        description =
                                "Pricing index 'B' (valeur indiciaire); invoice amount = B x the"
                                        + " payer's bRate (XOF per point).")
                @DecimalMin("0.0")
                @Nullable BigDecimal indiciaryValueB,
        @Schema(description = "Number of result labels to print.") @Min(0)
                @Nullable Integer labelCount,
        @Schema(description = "ULID of the sample matrix this analysis is performed on.")
                @Size(max = 26)
                @Nullable String matrixId,
        @Schema(description = "ULID of the default sample recipient/container for this analysis.")
                @Size(max = 26)
                @Nullable String recipientId,
        @Schema(
                        description =
                                "How the result is rendered/validated: FORMULA, INTEGER, REAL, TEXT"
                                        + " or FREE ('Autre').")
                @Nullable ResultType resultType,
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
