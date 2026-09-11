package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "Request to create a new medical specialty.")
public record CreateSpecialtyRequest(
        @Schema(description = "Admin-assigned specialty code; immutable after creation.")
                @NotBlank
                @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$")
                String code,
        @Schema(description = "Display name of the specialty.") @NotBlank @Size(max = 120)
                String name,
        @Schema(description = "Optional free-text description.") @Size(max = 255)
                @Nullable String description) {}
