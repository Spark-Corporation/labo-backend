package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

public record CreateSpecialtyRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$") String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 255) @Nullable String description) {}
