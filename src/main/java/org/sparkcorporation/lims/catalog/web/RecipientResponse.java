package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record RecipientResponse(
        String id,
        String code,
        String name,
        @Nullable String description,
        @Nullable String color,
        @Nullable BigDecimal volumeMl,
        @Nullable MatrixSnapshot defaultMatrix,
        @Nullable Instant deletedAt,
        Instant createdAt,
        Instant updatedAt,
        int version) {

    /** Flat snapshot of the defaultMatrix at read time — avoids exposing the full matrix DTO. */
    public record MatrixSnapshot(String id, String code, String name) {}
}
