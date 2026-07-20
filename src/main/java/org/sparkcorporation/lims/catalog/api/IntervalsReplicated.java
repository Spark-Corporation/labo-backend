package org.sparkcorporation.lims.catalog.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import org.sparkcorporation.lims.shared.domain.PatientCategory;

/**
 * Emitted when {@code ReferenceIntervalService.replicate(...)} replaces target-category intervals
 * with clones of the source-category intervals.
 *
 * <p>{@code targetCategories} is defensively copied via {@link Set#copyOf(java.util.Collection)} in
 * the compact constructor — the resulting set is unmodifiable, so subscribers can hold the event
 * safely.
 */
public record IntervalsReplicated(
        AnalysisId analysisId,
        PatientCategory sourceCategory,
        Set<PatientCategory> targetCategories,
        int count,
        Instant changedAt) {

    public IntervalsReplicated {
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(sourceCategory, "sourceCategory");
        Objects.requireNonNull(targetCategories, "targetCategories");
        Objects.requireNonNull(changedAt, "changedAt");
        targetCategories = Set.copyOf(targetCategories); // defensive copy → unmodifiable
    }
}
