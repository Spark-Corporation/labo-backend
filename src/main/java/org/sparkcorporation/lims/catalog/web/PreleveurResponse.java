package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.Habilitation;

@Schema(description = "A phlebotomist (préleveur) authorized to collect samples.")
public record PreleveurResponse(
        @Schema(description = "ULID of the préleveur.") String id,
        @Schema(description = "Server-generated préleveur code (format PHL-NNNNNN).") String code,
        @Schema(description = "Full name of the préleveur.") String name,
        @Schema(description = "Contact phone number.") @Nullable String phone,
        @Schema(description = "Contact email address.") @Nullable String email,
        @Schema(
                        description =
                                "ULID of the linked IAM user account, if this préleveur has a"
                                        + " system login.")
                @Nullable String userId,
        @Schema(description = "Phlebotomy-difficulty categories this préleveur is qualified for.")
                Set<Habilitation> habilitations,
        @Schema(description = "Timestamp the préleveur was created.") Instant createdAt,
        @Schema(description = "Timestamp the préleveur was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                long version) {}
