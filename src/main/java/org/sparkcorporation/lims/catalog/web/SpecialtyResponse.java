package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "A medical specialty that prescribers can be tagged with.")
public record SpecialtyResponse(
        @Schema(description = "ULID of the specialty.") String id,
        @Schema(description = "Admin-assigned specialty code.") String code,
        @Schema(description = "Display name of the specialty.") String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(description = "Timestamp the specialty was created.") Instant createdAt,
        @Schema(description = "Timestamp the specialty was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                long version) {}
