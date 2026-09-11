package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Immutable projection of an {@code Analysis} revision's own configuration fields (scalars +
 * embedded {@code Units}/{@code AnalyticalDelay}), used to compute the changed-field list between
 * two consecutive audit revisions (SP12 config history). Components, techniques and reference
 * intervals are audited as independent entities and surfaced as their own history timelines, not as
 * fields here.
 */
@Schema(description = "Snapshot of an analysis's own configuration fields as of one revision.")
public record AnalysisSnapshot(
        @Schema(description = "Analysis name.") String name,
        @Schema(description = "Indiciary value B (nomenclature pricing coefficient).")
                BigDecimal indiciaryValueB,
        @Schema(description = "Number of result labels billed for this analysis.") int labelCount,
        @Schema(description = "How results for this analysis are typed (numeric, text, etc.).")
                String resultType,
        @Schema(description = "Analysis kind (e.g. simple vs. grouped).") String kind,
        @Nullable @Schema(description = "Free-text interpretation guidance shown with the result.")
                String interpretationText,
        @Nullable
                @Schema(
                        description =
                                "Formula used to compute this analysis's value from its"
                                        + " components, if it is a computed analysis.")
                String formulaExpression,
        @Nullable
                @Schema(
                        description =
                                "Default rack (\"portoir\") samples for this analysis "
                                        + "are stored in.")
                String defaultPortoir,
        @Nullable
                @Schema(
                        description =
                                "Default number of days samples for this analysis are "
                                        + "conserved before destruction.")
                Integer defaultConservationDays,
        @Nullable @Schema(description = "Id of the default sample matrix for this analysis.")
                String matrixId,
        @Nullable @Schema(description = "Id of the default specimen container for this analysis.")
                String recipientId,
        @Nullable @Schema(description = "Primary unit results are reported in.") String unit1,
        @Nullable @Schema(description = "Number of decimal places for unit1 values.")
                Integer unit1Decimals,
        @Nullable @Schema(description = "Secondary unit results can also be reported in.")
                String unit2,
        @Nullable @Schema(description = "Number of decimal places for unit2 values.")
                Integer unit2Decimals,
        @Nullable @Schema(description = "Multiplier used to convert a unit1 value into unit2.")
                BigDecimal conversionFactor,
        @Nullable @Schema(description = "Result turnaround delay, in days.") Integer delayDays,
        @Nullable @Schema(description = "Result turnaround delay, additional hours.")
                Integer delayHours,
        @Nullable
                @Schema(
                        description =
                                "Number of anterior results to show for comparison, if "
                                        + "configured.")
                Integer anteriorityCount) {}
