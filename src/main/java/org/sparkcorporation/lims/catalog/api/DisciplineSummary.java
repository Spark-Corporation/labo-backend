package org.sparkcorporation.lims.catalog.api;

import org.jspecify.annotations.Nullable;

public record DisciplineSummary(
        DisciplineId id, String code, String name, @Nullable String description, boolean active) {}
