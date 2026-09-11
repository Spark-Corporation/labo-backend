package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * PATCH semantics: all fields nullable; null = no change. {@code code} is immutable — the
 * controller rejects PATCH bodies that include a {@code code} field with 422 (spec §5.5).
 */
@Schema(
        description =
                "Request to partially update a medical specialty; null fields are left unchanged."
                        + " 'code' is immutable and not part of this shape.")
public record UpdateSpecialtyRequest(
        @Schema(description = "Display name of the specialty.") @Size(max = 120)
                @Nullable String name,
        @Schema(description = "Optional free-text description.") @Size(max = 255)
                @Nullable String description) {}
