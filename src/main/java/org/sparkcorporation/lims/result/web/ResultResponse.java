package org.sparkcorporation.lims.result.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Web-tier mirror of {@link org.sparkcorporation.lims.result.api.ResultSummary}. Carries flat
 * snapshots plus the two child collections.
 */
@Schema(
        description =
                "Result aggregate: one row per analysis request, with its lines and antibiogram"
                        + " readings")
public record ResultResponse(
        @Schema(description = "ULID of this result") String id,
        @Schema(description = "ULID of the lab file (dossier) this result belongs to")
                String labFileId,
        @Schema(description = "ULID of the originating analysis request") String analysisRequestId,
        @Schema(description = "ULID of the catalog analysis this result was created for")
                String analysisId,
        @Schema(description = "Analysis code snapshotted at result-creation time")
                String analysisCodeSnapshot,
        @Schema(
                        description =
                                "Result lifecycle state: DRAFT (incomplete), ENTERED (all lines"
                                        + " filled), TECHNICAL_VALIDATED, or BIOLOGICAL_VALIDATED"
                                        + " (terminal)")
                String state,
        @Schema(
                        description =
                                "User id of the technician who technically validated this result,"
                                        + " if any")
                @Nullable String technicalValidatedBy,
        @Schema(description = "Timestamp of technical validation, if any")
                @Nullable Instant technicalValidatedAt,
        @Schema(
                        description =
                                "User id of the biologist who biologically validated this result,"
                                        + " if any")
                @Nullable String biologicalValidatedBy,
        @Schema(description = "Timestamp of biological validation, if any")
                @Nullable Instant biologicalValidatedAt,
        @Schema(description = "One line per analysis component, in position order")
                List<LineResponse> lines,
        @Schema(
                        description =
                                "Antibiogram readings recorded against this result, in position"
                                        + " order")
                List<AntibiogramReadingResponse> antibiogramReadings,
        @Schema(description = "Timestamp this result was created") Instant createdAt,
        @Schema(description = "User id who created this result, if known")
                @Nullable String createdByUserId) {

    public ResultResponse {
        lines = List.copyOf(lines);
        antibiogramReadings = List.copyOf(antibiogramReadings);
    }

    /** Web-tier mirror of {@code ResultSummary.LineSummary}. */
    @Schema(
            description =
                    "One result line: polymorphic typed value for a single analysis component")
    public record LineResponse(
            @Schema(description = "ULID of this line") String id,
            @Schema(description = "ULID of the catalog analysis component this line reports on")
                    String componentAnalysisId,
            @Schema(description = "Component code snapshotted at result-creation time")
                    String componentCodeSnapshot,
            @Schema(description = "Display order of this line within the result") int position,
            @Schema(
                            description =
                                    "Value kind of this line: INTEGER, REAL, TEXT, FORMULA"
                                            + " (server-computed), or FREE")
                    String resultType,
            @Schema(description = "Value when resultType is INTEGER") @Nullable Long valueInteger,
            @Schema(description = "Value when resultType is REAL or FORMULA")
                    @Nullable BigDecimal valueReal,
            @Schema(description = "Value when resultType is TEXT or FREE")
                    @Nullable String valueText,
            @Schema(description = "Primary unit snapshotted for this component")
                    @Nullable String unit1Snapshot,
            @Schema(description = "Decimal places to display for the primary unit")
                    @Nullable Integer unit1Decimals,
            @Schema(description = "Secondary (converted) unit snapshotted for this component")
                    @Nullable String unit2Snapshot,
            @Schema(description = "Decimal places to display for the secondary unit")
                    @Nullable Integer unit2Decimals,
            @Schema(
                            description =
                                    "ULID of the reference interval matched against the value, if"
                                            + " any")
                    @Nullable String referenceIntervalId,
            @Schema(description = "Whether the value falls outside the matched reference interval")
                    boolean isOutOfRange,
            @Schema(description = "Free-text comment attached to this line, if any")
                    @Nullable String commentText) {}

    /** Web-tier mirror of {@code ResultSummary.AntibiogramReadingSummary}. */
    @Schema(
            description =
                    "One antibiogram reading: an antibiotic's measured diameter and interpreted"
                            + " sensitivity")
    public record AntibiogramReadingResponse(
            @Schema(description = "ULID of this reading") String id,
            @Schema(description = "ULID of the catalog antibiotic tested") String antibioticId,
            @Schema(
                            description =
                                    "Measured inhibition zone diameter in millimeters, as a decimal"
                                            + " string")
                    String measuredDiameter,
            @Schema(
                            description =
                                    "Sensitivity interpretation (Sensitive/Intermediate/Resistant)"
                                            + " derived from the antibiotic's catalog breakpoints")
                    String interpretedSensitivity,
            @Schema(description = "Display order of this reading within the result")
                    int position) {}
}
