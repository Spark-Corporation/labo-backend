package org.sparkcorporation.lims.catalog.web;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.PatientCategory;

/**
 * Request body for {@code POST /api/v1/catalog/analyses/{id}/intervals:replicate}.
 *
 * <p>{@code targetCategories} is optional — when null/empty, defaults to "all 3 other categories"
 * per spec §5.7.
 */
public record ReplicateIntervalsRequest(
        @NotNull PatientCategory sourceCategory, @Nullable Set<PatientCategory> targetCategories) {}
