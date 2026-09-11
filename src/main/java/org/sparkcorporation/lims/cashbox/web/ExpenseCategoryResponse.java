package org.sparkcorporation.lims.cashbox.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An expense classification used when recording cashbox expenses")
public record ExpenseCategoryResponse(
        @Schema(description = "ULID of this category") String id,
        @Schema(description = "Uppercase-normalized, immutable category code") String code,
        @Schema(description = "Display label") String label,
        @Schema(description = "Whether the category can currently be used to record new expenses")
                boolean active,
        @Schema(description = "Timestamp the category was created") Instant createdAt,
        @Schema(description = "Timestamp the category was last updated") Instant updatedAt) {}
