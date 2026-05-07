package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for {@code POST /api/v1/catalog/analyses/{id}:duplicate}. */
public record DuplicateAnalysisRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$") String newCode) {}
