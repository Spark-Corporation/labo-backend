package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;

/**
 * Response DTO mirroring {@link org.sparkcorporation.lims.catalog.api.AnalysisSummary} with audit
 * fields ({@code createdAt}, {@code updatedAt}, {@code version}) added.
 */
@Schema(description = "Full configuration of a catalog analysis.")
public record AnalysisResponse(
        @Schema(description = "ULID of the analysis.") String id,
        @Schema(description = "Admin-assigned, immutable analysis code.") String code,
        @Schema(description = "Display name of the analysis.") String name,
        @Schema(
                        description =
                                "Discriminator: SIMPLE (leaf), GROUPED (bundles SIMPLE components)"
                                        + " or SOCIETY_PANEL.")
                AnalysisKind kind,
        @Schema(
                        description =
                                "Pricing index 'B' (valeur indiciaire); invoice amount = B x the"
                                        + " payer's bRate (XOF per point).")
                BigDecimal indiciaryValueB,
        @Schema(
                        description =
                                "Number of result labels printed for this analysis; defaults to 1"
                                        + " at creation.")
                int labelCount,
        @Schema(description = "Snapshot of the discipline this analysis belongs to.")
                DisciplineSnapshotResponse discipline,
        @Schema(
                        description =
                                "Snapshot of the sample matrix this analysis is performed on, if"
                                        + " any.")
                @Nullable MatrixSnapshotResponse matrix,
        @Schema(
                        description =
                                "Snapshot of the default sample recipient/container for this"
                                        + " analysis, if any.")
                @Nullable RecipientSnapshotResponse recipient,
        @Schema(
                        description =
                                "How the result is rendered/validated: FORMULA, INTEGER, REAL, TEXT"
                                        + " or FREE ('Autre').")
                ResultType resultType,
        @Schema(description = "Free-text clinical interpretation text shown alongside the result.")
                @Nullable String interpretationText,
        @Schema(
                        description =
                                "Formula expression used to compute the result (only relevant when"
                                        + " resultType is FORMULA).")
                @Nullable String formulaExpression,
        @Schema(
                        description =
                                "Default sample rack/carrier (portoir) identifier assigned at"
                                        + " collection.")
                @Nullable String defaultPortoir,
        @Schema(
                        description =
                                "Default number of days a sample is conserved before its"
                                    + " destruction is due; snapshotted at sample collection time.")
                @Nullable Integer defaultConservationDays,
        @Schema(description = "Result unit configuration.") UnitsResponse units,
        @Schema(description = "Expected result turnaround delay.") AnalyticalDelayResponse delay,
        @Schema(
                        description =
                                "Child SIMPLE analyses composing this GROUPED/SOCIETY_PANEL"
                                        + " analysis, in display order.")
                List<ComponentLinkResponse> components,
        @Schema(
                        description =
                                "Analytical techniques linked to this analysis, at most one marked"
                                        + " default.")
                List<TechniqueLinkResponse> techniques,
        @Schema(description = "Timestamp the analysis was created.") Instant createdAt,
        @Schema(description = "Timestamp the analysis was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version, incremented on every update.")
                long version) {

    @Schema(
            description =
                    "Flat snapshot of the linked discipline embedded in the analysis response.")
    public record DisciplineSnapshotResponse(
            @Schema(description = "ULID of the discipline.") String id,
            @Schema(description = "Code of the discipline.") String code,
            @Schema(description = "Name of the discipline.") String name) {}

    @Schema(description = "Flat snapshot of the linked matrix embedded in the analysis response.")
    public record MatrixSnapshotResponse(
            @Schema(description = "ULID of the matrix.") String id,
            @Schema(description = "Code of the matrix.") String code,
            @Schema(description = "Name of the matrix.") String name) {}

    @Schema(
            description =
                    "Flat snapshot of the linked recipient embedded in the analysis response.")
    public record RecipientSnapshotResponse(
            @Schema(description = "ULID of the recipient.") String id,
            @Schema(description = "Code of the recipient.") String code,
            @Schema(description = "Name of the recipient.") String name) {}

    @Schema(
            description =
                    "Result unit configuration; supports an optional secondary unit derived by a"
                            + " conversion factor.")
    public record UnitsResponse(
            @Schema(description = "Primary result unit label (e.g. 'g/L').") @Nullable String unit1,
            @Schema(description = "Number of decimal places to display for unit1, in [0,5].")
                    @Nullable Integer unit1Decimals,
            @Schema(
                            description =
                                    "Secondary result unit label, if the result is also expressed"
                                            + " in another unit.")
                    @Nullable String unit2,
            @Schema(description = "Number of decimal places to display for unit2, in [0,5].")
                    @Nullable Integer unit2Decimals,
            @Schema(description = "Multiplier used to convert a value expressed in unit1 to unit2.")
                    @Nullable BigDecimal conversionFactor) {}

    @Schema(description = "Expected turnaround delay for the analysis result.")
    public record AnalyticalDelayResponse(
            @Schema(description = "Number of whole days of turnaround delay.")
                    @Nullable Integer days,
            @Schema(description = "Additional hours of turnaround delay, on top of 'days'.")
                    @Nullable Integer hours,
            @Schema(
                            description =
                                    "Number of prior/anteriority results required before this delay"
                                            + " applies.")
                    @Nullable Integer anteriorityCount) {}
}
