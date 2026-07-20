package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

public record CreateDisciplineRequest(
        @NotBlank @Size(max = 30) @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,29}$") String code,
        @NotBlank @Size(max = 120) String name,
        @Nullable String description,
        @NotNull DisciplinePrefix disciplinePrefix) {}
