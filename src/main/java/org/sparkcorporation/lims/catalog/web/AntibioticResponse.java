package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "Antibiotic catalog entry used for antibiogram (susceptibility) reporting.")
public record AntibioticResponse(
        @Schema(description = "ULID of the antibiotic.") String id,
        @Schema(description = "Admin-assigned code of the antibiotic.") String code,
        @Schema(description = "Display name of the antibiotic.") String name,
        @Schema(description = "Optional free-text description.") @Nullable String description,
        @Schema(description = "Antibiotic family/class (e.g. beta-lactams), if classified.")
                @Nullable String family,
        @Schema(
                        description =
                                "Reference inhibition-zone diameter (mm) used to classify a strain"
                                        + " as susceptible.")
                BigDecimal referenceDiameter,
        @Schema(
                        description =
                                "Lower bound (mm) of the intermediate-susceptibility diameter"
                                        + " range, if defined.")
                @Nullable BigDecimal intermediateLower,
        @Schema(
                        description =
                                "Upper bound (mm) of the intermediate-susceptibility diameter"
                                        + " range, if defined.")
                @Nullable BigDecimal intermediateUpper,
        @Schema(description = "Timestamp the antibiotic was soft-deleted, or null if active.")
                @Nullable Instant deletedAt,
        @Schema(description = "Timestamp the antibiotic was created.") Instant createdAt,
        @Schema(description = "Timestamp the antibiotic was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                int version) {}
