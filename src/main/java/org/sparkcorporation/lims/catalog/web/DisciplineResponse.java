package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

@Schema(
        description =
                "A medical discipline (e.g. Biochimie, Hématologie) used to classify analyses and"
                        + " prefix sample label ids.")
public record DisciplineResponse(
        @Schema(description = "ULID of the discipline.") String id,
        @Schema(description = "Admin-assigned discipline code.") String code,
        @Schema(description = "Display name of the discipline.") String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(
                        description =
                                "One of the five regulatory disciplines whose numeric prefix is"
                                        + " used to build sample label ids.")
                DisciplinePrefix prefix,
        @Schema(description = "Timestamp the discipline was soft-deleted, or null if active.")
                @Nullable Instant deletedAt,
        @Schema(description = "Timestamp the discipline was created.") Instant createdAt,
        @Schema(description = "Timestamp the discipline was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                int version) {}
