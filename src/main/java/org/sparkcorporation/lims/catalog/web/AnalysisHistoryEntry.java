package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "One audited change to an analysis's configuration.")
public record AnalysisHistoryEntry(
        @Schema(description = "Revision number this entry represents, increasing per change.")
                long revision,
        @Schema(description = "When this revision was recorded.") Instant timestamp,
        @Nullable
                @Schema(
                        description =
                                "Id of the user who made the change; absent for system"
                                        + " or anonymous writes.")
                String userId,
        @Schema(description = "Which part of the analysis configuration this entry describes.")
                Scope scope,
        @Schema(description = "Names of the fields that changed in this revision.")
                List<String> changedFields,
        @Nullable
                @Schema(
                        description =
                                "Post-change state: an AnalysisSnapshot object when scope is"
                                    + " ANALYSIS, or a list of ChildChange objects for the other"
                                    + " scopes.")
                Object snapshot) {

    /** Which part of the analysis configuration a history entry describes. */
    public enum Scope {
        ANALYSIS,
        REFERENCE_INTERVALS,
        COMPONENTS,
        TECHNIQUES
    }
}
