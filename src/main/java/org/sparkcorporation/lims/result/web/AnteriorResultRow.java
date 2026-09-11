package org.sparkcorporation.lims.result.web;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;

/**
 * Row shape for the §IV anterior-results lookup ({@code GET /api/v1/results/anterior}). Not
 * paginated — returned as a bounded {@code List} directly.
 */
@Schema(
        description =
                "One of a patient's most recent biologically-validated results for a given "
                        + "component, used to show trends against the current result.")
public record AnteriorResultRow(
        @Schema(description = "Id of the anterior result.") ResultId resultId,
        @Schema(description = "Id of the anterior result line.") ResultLineId lineId,
        @Schema(description = "Id of the lab file the anterior result belongs to.")
                LabFileId labFileId,
        @Schema(
                        description =
                                "Registration number of the lab file the anterior result "
                                        + "belongs to.")
                String labFileRegistrationNumber,
        @Schema(description = "Code of the analysis component this line reports.")
                String componentCode,
        @Schema(description = "How the value below is typed: numeric, text, etc.")
                ResultType resultType,
        @Nullable @Schema(description = "Integer value, set when resultType is an integer type.")
                Long valueInteger,
        @Nullable @Schema(description = "Decimal value, set when resultType is a decimal type.")
                BigDecimal valueReal,
        @Nullable @Schema(description = "Free-text value, set when resultType is text.")
                String valueText,
        @Nullable
                @Schema(
                        description =
                                "Unit for valueInteger/valueReal, snapshotted at entry " + "time.")
                String unit1Snapshot,
        @Schema(description = "Whether the value fell outside its reference interval.")
                boolean isOutOfRange,
        @Schema(description = "When this anterior result was biologically validated.")
                Instant biologicalValidatedAt) {}
