package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record MatrixResponse(
        String id,
        String code,
        String name,
        @Nullable String description,
        @Nullable Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        int version) {}
