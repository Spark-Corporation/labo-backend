package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/** Response DTO for a {@code ReferenceInterval}. */
@Schema(
        description =
                "A reference (normal-range) interval for an analysis, scoped to a patient"
                        + " category.")
public record ReferenceIntervalResponse(
        @Schema(description = "ULID of the reference interval.") String id,
        @Schema(description = "ULID of the analysis this interval belongs to.") String analysisId,
        @Schema(
                        description =
                                "Patient category this interval applies to (HOMME, FEMME, ENFANT,"
                                        + " NOURRISSON_NOUVEAU_NE).")
                PatientCategory category,
        @Schema(
                        description =
                                "Biological sex this interval applies to, if further restricted"
                                        + " within the category.")
                @Nullable Sex sex,
        @Schema(description = "Minimum patient age this interval applies to, in ageUnit units.")
                @Nullable Integer ageMin,
        @Schema(description = "Maximum patient age this interval applies to, in ageUnit units.")
                @Nullable Integer ageMax,
        @Schema(description = "Unit (DAYS, MONTHS, YEARS) for ageMin/ageMax.")
                @Nullable AgeUnit ageUnit,
        @Schema(
                        description =
                                "How this interval is rendered: STANDARD (numeric bounds), COMMENT"
                                        + " (free text) or NONE.")
                ReferenceMode mode,
        @Schema(description = "Lower bound of the normal range expressed in unit1 (STANDARD mode).")
                @Nullable BigDecimal unit1Min,
        @Schema(description = "Upper bound of the normal range expressed in unit1 (STANDARD mode).")
                @Nullable BigDecimal unit1Max,
        @Schema(description = "Lower bound of the normal range expressed in unit2 (STANDARD mode).")
                @Nullable BigDecimal unit2Min,
        @Schema(description = "Upper bound of the normal range expressed in unit2 (STANDARD mode).")
                @Nullable BigDecimal unit2Max,
        @Schema(
                        description =
                                "Free-text commentary shown instead of numeric bounds (COMMENT"
                                        + " mode).")
                @Nullable String commentText,
        @Schema(description = "Timestamp the interval was created.") Instant createdAt,
        @Schema(description = "Timestamp the interval was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                long version) {}
