package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record SpecialtyResponse(
        String id,
        String code,
        String name,
        @Nullable String description,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
