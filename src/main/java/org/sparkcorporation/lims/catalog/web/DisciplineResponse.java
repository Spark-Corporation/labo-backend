package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

public record DisciplineResponse(
        String id,
        String code,
        String name,
        @Nullable String description,
        DisciplinePrefix prefix,
        @Nullable Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        int version) {}
