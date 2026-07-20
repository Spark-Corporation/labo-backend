package org.sparkcorporation.lims.catalog.api;

import org.jspecify.annotations.Nullable;

public record SpecialtySummary(
        SpecialtyId id, String code, String name, @Nullable String description) {}
