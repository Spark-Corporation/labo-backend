package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "An analytical technique that can be linked to analyses.")
public record TechniqueResponse(
        @Schema(description = "ULID of the technique.") String id,
        @Schema(description = "Admin-assigned technique code.") String code,
        @Schema(description = "Display name of the technique.") String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(description = "Timestamp the technique was soft-deleted, or null if active.")
                @Nullable Instant deletedAt,
        @Schema(description = "Timestamp the technique was created.") Instant createdAt,
        @Schema(description = "Timestamp the technique was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                int version) {}
