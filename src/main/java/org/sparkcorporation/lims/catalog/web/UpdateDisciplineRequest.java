package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(
        description =
                "Request to partially update a discipline; null fields are left unchanged. The"
                        + " discipline prefix is immutable and not part of this shape.")
public record UpdateDisciplineRequest(
        @Schema(description = "Display name of the discipline.") @Size(max = 120)
                @Nullable String name,
        @Schema(description = "Optional free-text description.") @Nullable String description) {}
