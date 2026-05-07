package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for {@code POST /api/v1/catalog/analyses/{id}/components}. */
public record AddComponentRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$") String childCode,
        @Min(1) int position) {}
