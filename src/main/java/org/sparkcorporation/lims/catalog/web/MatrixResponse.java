package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "A sample matrix (specimen type), e.g. blood or urine.")
public record MatrixResponse(
        @Schema(description = "ULID of the matrix.") String id,
        @Schema(description = "Admin-assigned matrix code.") String code,
        @Schema(description = "Display name of the matrix.") String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(description = "Timestamp the matrix was soft-deleted, or null if active.")
                @Nullable Instant deletedAt,
        @Schema(description = "Timestamp the matrix was created.") Instant createdAt,
        @Schema(description = "Timestamp the matrix was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                int version) {}
