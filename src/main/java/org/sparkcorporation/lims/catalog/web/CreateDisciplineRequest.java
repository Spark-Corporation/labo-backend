package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

@Schema(description = "Request to create a new medical discipline.")
public record CreateDisciplineRequest(
        @Schema(description = "Admin-assigned discipline code.")
                @NotBlank
                @Size(max = 30)
                @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$")
                String code,
        @Schema(description = "Display name of the discipline.") @NotBlank @Size(max = 120)
                String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(
                        description =
                                "One of the five regulatory disciplines whose numeric prefix is"
                                    + " used to build sample label ids; immutable after creation.")
                @NotNull
                DisciplinePrefix disciplinePrefix) {}
