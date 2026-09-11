package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "A prescribing physician.")
public record PrescriberResponse(
        @Schema(description = "ULID of the prescriber.") String id,
        @Schema(description = "Server-generated prescriber code (format PRS-NNNNNN).") String code,
        @Schema(description = "Courtesy title (e.g. 'Dr').") @Nullable String title,
        @Schema(description = "Full name of the prescriber.") String name,
        @Schema(description = "Snapshot of the prescriber's linked medical specialty, if any.")
                @Nullable SpecialtySnapshotResponse specialty,
        @Schema(description = "Contact phone number.") @Nullable String phone,
        @Schema(description = "Contact email address.") @Nullable String email,
        @Schema(description = "Professional medical license number.")
                @Nullable String licenseNumber,
        @Schema(description = "Name of the institution the prescriber is affiliated with.")
                @Nullable String institution,
        @Schema(description = "Timestamp the prescriber was created.") Instant createdAt,
        @Schema(description = "Timestamp the prescriber was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                long version) {

    /** Flat snapshot of the linked specialty embedded in the prescriber response. */
    @Schema(
            description =
                    "Flat snapshot of the linked medical specialty embedded in the prescriber"
                            + " response.")
    public record SpecialtySnapshotResponse(
            @Schema(description = "ULID of the specialty.") String id,
            @Schema(description = "Code of the specialty.") String code,
            @Schema(description = "Name of the specialty.") String name) {}
}
