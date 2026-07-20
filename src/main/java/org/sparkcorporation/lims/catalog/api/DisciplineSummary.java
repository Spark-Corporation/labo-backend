package org.sparkcorporation.lims.catalog.api;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

public record DisciplineSummary(
        DisciplineId id,
        String code,
        String name,
        @Nullable String description,
        boolean active,
        DisciplinePrefix prefix) {}
