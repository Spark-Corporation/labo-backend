package org.sparkcorporation.lims.catalog.api;

import org.jspecify.annotations.Nullable;

public record MatrixSummary(
        MatrixId id, String code, String name, @Nullable String description, boolean active) {}
