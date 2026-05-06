package org.sparkcorporation.lims.catalog.api;

import org.jspecify.annotations.Nullable;

public record TechniqueSummary(
        TechniqueId id, String code, String name, @Nullable String description, boolean active) {}
