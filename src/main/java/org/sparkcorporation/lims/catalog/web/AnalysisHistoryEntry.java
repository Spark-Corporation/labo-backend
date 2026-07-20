package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * One entry in an analysis's configuration-modification history (SP12, CDC Configurations §8/§9):
 * who ({@code userId}, a user ULID — null for system/anonymous writes), when ({@code timestamp}),
 * and what ({@code changedFields}) changed in revision {@code revision}, with a typed {@code
 * snapshot} of the post-change state.
 *
 * <p>For {@link Scope#ANALYSIS} the snapshot is an {@link AnalysisSnapshot}; for the sub-aggregate
 * scopes it is a {@code List<}{@link ChildChange}{@code >} of the rows that changed in that
 * revision.
 */
public record AnalysisHistoryEntry(
        long revision,
        Instant timestamp,
        @Nullable String userId,
        Scope scope,
        List<String> changedFields,
        @Nullable Object snapshot) {

    /** Which part of the analysis configuration a history entry describes. */
    public enum Scope {
        ANALYSIS,
        REFERENCE_INTERVALS,
        COMPONENTS,
        TECHNIQUES
    }
}
