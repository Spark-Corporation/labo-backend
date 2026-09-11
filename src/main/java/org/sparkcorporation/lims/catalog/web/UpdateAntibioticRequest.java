package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "Request to partially update an antibiotic; null fields are left unchanged.")
public record UpdateAntibioticRequest(
        @Schema(description = "Display name of the antibiotic.") @Size(max = 120)
                @Nullable String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(description = "Antibiotic family/class (e.g. beta-lactams).") @Size(max = 80)
                @Nullable String family,
        @Schema(
                        description =
                                "Reference inhibition-zone diameter (mm) used to classify a strain"
                                        + " as susceptible.")
                @DecimalMin("0.0")
                @Digits(integer = 3, fraction = 2)
                @Nullable BigDecimal referenceDiameter,
        @Schema(description = "Lower bound (mm) of the intermediate-susceptibility diameter range.")
                @DecimalMin("0.0")
                @Digits(integer = 3, fraction = 2)
                @Nullable BigDecimal intermediateLower,
        @Schema(description = "Upper bound (mm) of the intermediate-susceptibility diameter range.")
                @DecimalMin("0.0")
                @Digits(integer = 3, fraction = 2)
                @Nullable BigDecimal intermediateUpper) {}
