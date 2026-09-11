package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(
        description =
                "Request to partially update a sample matrix; null fields are left unchanged.")
public record UpdateMatrixRequest(
        @Schema(description = "Display name of the matrix.") @Size(max = 120) @Nullable String name,
        @Schema(description = "Optional free-text description.") @Nullable String description) {}
