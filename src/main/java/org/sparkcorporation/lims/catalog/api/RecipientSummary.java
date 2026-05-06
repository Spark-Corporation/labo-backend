package org.sparkcorporation.lims.catalog.api;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record RecipientSummary(
        RecipientId id,
        String code,
        String name,
        @Nullable String description,
        @Nullable String color,
        @Nullable BigDecimal volumeMl,
        @Nullable MatrixSnapshot defaultMatrix,
        boolean active) {

    /** Flat snapshot of the defaultMatrix at read time — avoids cross-module N+1. */
    public record MatrixSnapshot(MatrixId id, String code, String name) {}
}
