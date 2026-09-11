package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "Request to create a new sample matrix (specimen type).")
public record CreateMatrixRequest(
        @Schema(description = "Admin-assigned matrix code.")
                @NotBlank
                @Size(max = 30)
                @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$")
                String code,
        @Schema(description = "Display name of the matrix (e.g. 'Blood', 'Urine').")
                @NotBlank
                @Size(max = 120)
                String name,
        @Schema(description = "Optional free-text description.") @Nullable String description) {}
