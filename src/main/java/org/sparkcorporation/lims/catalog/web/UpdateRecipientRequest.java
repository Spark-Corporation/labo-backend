package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(
        description =
                "Request to partially update a sample recipient; null fields are left unchanged.")
public record UpdateRecipientRequest(
        @Schema(description = "Display name of the recipient.") @Size(max = 120)
                @Nullable String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(description = "Cap/label color used to visually identify this recipient.")
                @Size(max = 30)
                @Nullable String color,
        @Schema(description = "Fill volume of the recipient, in milliliters.")
                @DecimalMin("0.0")
                @Digits(integer = 4, fraction = 2)
                @Nullable BigDecimal volumeMl,
        @Schema(description = "ULID of the sample matrix this recipient is normally used with.")
                @Nullable String defaultMatrixId) {}
