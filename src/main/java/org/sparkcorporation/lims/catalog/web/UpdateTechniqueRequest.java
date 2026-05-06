package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

public record UpdateTechniqueRequest(
        @Size(max = 120) @Nullable String name, @Nullable String description) {}
