package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "Request to create a new antibiotic catalog entry.")
public record CreateAntibioticRequest(
        @Schema(description = "Admin-assigned antibiotic code.")
                @NotBlank
                @Size(max = 30)
                @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$")
                String code,
        @Schema(description = "Display name of the antibiotic.") @NotBlank @Size(max = 120)
                String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(description = "Antibiotic family/class (e.g. beta-lactams), if classified.")
                @Size(max = 80)
                @Nullable String family,
        @Schema(
                        description =
                                "Reference inhibition-zone diameter (mm) used to classify a strain"
                                        + " as susceptible.")
                @NotNull
                @DecimalMin("0.0")
                @Digits(integer = 3, fraction = 2)
                BigDecimal referenceDiameter,
        @Schema(
                        description =
                                "Lower bound (mm) of the intermediate-susceptibility diameter"
                                        + " range, if defined.")
                @DecimalMin("0.0")
                @Digits(integer = 3, fraction = 2)
                @Nullable BigDecimal intermediateLower,
        @Schema(
                        description =
                                "Upper bound (mm) of the intermediate-susceptibility diameter"
                                        + " range, if defined.")
                @DecimalMin("0.0")
                @Digits(integer = 3, fraction = 2)
                @Nullable BigDecimal intermediateUpper) {}
