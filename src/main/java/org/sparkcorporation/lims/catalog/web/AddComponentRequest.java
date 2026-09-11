package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;

/** Request body for {@code POST /api/v1/catalog/analyses/{id}/components}. */
@Schema(
        description =
                "Request to attach a SIMPLE analysis as a component of a GROUPED or SOCIETY_PANEL"
                        + " analysis.")
public record AddComponentRequest(
        @Schema(description = "Code of the SIMPLE analysis to attach as a child component.")
                @NotBlank
                @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$")
                String childCode,
        @Schema(
                        description =
                                "1-based display position of the component within the parent's"
                                        + " component list.")
                @Min(1)
                int position) {}
