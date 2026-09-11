package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Request body for {@code PUT /api/v1/catalog/analyses/{id}/intervals}.
 *
 * <p>Full-replace semantics: existing intervals soft-deleted, new ones persisted. Mode-specific
 * invariants validated by {@code ReferenceInterval.create}.
 */
@Schema(description = "Request to fully replace the reference intervals of an analysis.")
public record PutIntervalsRequest(
        @Schema(
                        description =
                                "Reference intervals to persist; replaces the entire existing set"
                                        + " for the analysis.")
                @NotNull
                @Valid
                List<@Valid IntervalPayload> items) {

    @Schema(
            description =
                    "One reference interval, scoped to a patient category and optionally a sex/age"
                            + " range.")
    public record IntervalPayload(
            @Schema(
                            description =
                                    "Patient category this interval applies to (HOMME, FEMME,"
                                            + " ENFANT, NOURRISSON_NOUVEAU_NE).")
                    @NotNull
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
                                    "How this interval is rendered: STANDARD (numeric bounds),"
                                            + " COMMENT (free text) or NONE.")
                    @NotNull
                    ReferenceMode mode,
            @Schema(
                            description =
                                    "Lower bound of the normal range expressed in unit1 (STANDARD"
                                            + " mode).")
                    @Nullable BigDecimal unit1Min,
            @Schema(
                            description =
                                    "Upper bound of the normal range expressed in unit1 (STANDARD"
                                            + " mode).")
                    @Nullable BigDecimal unit1Max,
            @Schema(
                            description =
                                    "Lower bound of the normal range expressed in unit2 (STANDARD"
                                            + " mode).")
                    @Nullable BigDecimal unit2Min,
            @Schema(
                            description =
                                    "Upper bound of the normal range expressed in unit2 (STANDARD"
                                            + " mode).")
                    @Nullable BigDecimal unit2Max,
            @Schema(
                            description =
                                    "Free-text commentary shown instead of numeric bounds (COMMENT"
                                            + " mode).")
                    @Nullable String commentText) {}
}
