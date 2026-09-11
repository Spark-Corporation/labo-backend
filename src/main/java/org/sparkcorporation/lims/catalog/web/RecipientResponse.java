package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "A sample recipient (container/tube type) used to collect specimens.")
public record RecipientResponse(
        @Schema(description = "ULID of the recipient.") String id,
        @Schema(description = "Admin-assigned recipient code.") String code,
        @Schema(description = "Display name of the recipient.") String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(description = "Cap/label color used to visually identify this recipient.")
                @Nullable String color,
        @Schema(description = "Fill volume of the recipient, in milliliters.")
                @Nullable BigDecimal volumeMl,
        @Schema(
                        description =
                                "Snapshot of the sample matrix this recipient is normally used"
                                        + " with, if any.")
                @Nullable MatrixSnapshot defaultMatrix,
        @Schema(description = "Timestamp the recipient was soft-deleted, or null if active.")
                @Nullable Instant deletedAt,
        @Schema(description = "Timestamp the recipient was created.") Instant createdAt,
        @Schema(description = "Timestamp the recipient was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                int version) {

    /** Flat snapshot of the defaultMatrix at read time — avoids exposing the full matrix DTO. */
    @Schema(
            description =
                    "Flat snapshot of the linked default matrix embedded in the recipient"
                            + " response.")
    public record MatrixSnapshot(
            @Schema(description = "ULID of the matrix.") String id,
            @Schema(description = "Code of the matrix.") String code,
            @Schema(description = "Name of the matrix.") String name) {}
}
