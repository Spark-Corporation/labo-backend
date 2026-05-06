package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record TechniqueResponse(
        String id,
        String code,
        String name,
        @Nullable String description,
        @Nullable Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        int version) {}
