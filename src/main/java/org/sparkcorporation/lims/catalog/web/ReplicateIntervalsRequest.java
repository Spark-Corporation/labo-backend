package org.sparkcorporation.lims.catalog.web;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.PatientCategory;

/**
 * Request body for {@code POST /api/v1/catalog/analyses/{id}/intervals:replicate}.
 *
 * <p>{@code targetCategories} is optional — when null/empty, defaults to "all 3 other categories"
 * per spec §5.7.
 */
@Schema(
        description =
                "Request to copy the reference intervals of one patient category onto other"
                        + " categories.")
public record ReplicateIntervalsRequest(
        @Schema(description = "Patient category whose reference intervals are copied from.")
                @NotNull
                PatientCategory sourceCategory,
        @Schema(
                        description =
                                "Patient categories to copy the intervals onto, overwriting any"
                                    + " existing intervals there; when omitted or empty, defaults"
                                    + " to the other three categories.")
                @Nullable Set<PatientCategory> targetCategories) {}
