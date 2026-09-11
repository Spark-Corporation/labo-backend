package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;

/** Request body for {@code POST /api/v1/catalog/analyses/{id}:duplicate}. */
@Schema(description = "Request to clone an analysis under a new code.")
public record DuplicateAnalysisRequest(
        @Schema(description = "Code to assign to the cloned analysis; must not already be in use.")
                @NotBlank
                @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$")
                String newCode) {}
