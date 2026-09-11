package org.sparkcorporation.lims.result.web;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;

@Schema(description = "One matching result line from a multi-criteria result search.")
public record ResultSearchRow(
        @Schema(description = "Id of the result the line belongs to.") ResultId resultId,
        @Schema(description = "Id of this result line.") ResultLineId lineId,
        @Schema(description = "Id of the lab file the result belongs to.") LabFileId labFileId,
        @Schema(description = "Lab file's registration number.") String labFileRegistrationNumber,
        @Schema(description = "Id of the patient the result belongs to.") PatientId patientId,
        @Nullable @Schema(description = "Patient's lab-internal code, if known.")
                String patientLabCode,
        @Nullable @Schema(description = "Patient's last name, if known.") String patientLastName,
        @Nullable @Schema(description = "Patient's first name(s), if known.")
                String patientFirstNames,
        @Schema(description = "Whether the patient is covered by insurance.")
                boolean patientInsured,
        @Schema(description = "Code of the analysis this line belongs to.") String analysisCode,
        @Schema(description = "Code of the analysis component this line reports.")
                String componentCode,
        @Schema(description = "1-based position of this line within the result.") int position,
        @Schema(description = "How the value below is typed: numeric, text, etc.")
                ResultType resultType,
        @Nullable @Schema(description = "Integer value, set when resultType is an integer type.")
                Long valueInteger,
        @Nullable @Schema(description = "Decimal value, set when resultType is a decimal type.")
                BigDecimal valueReal,
        @Nullable @Schema(description = "Free-text value, set when resultType is text.")
                String valueText,
        @Nullable
                @Schema(description = "Unit for valueInteger/valueReal, snapshotted at entry time.")
                String unit1Snapshot,
        @Schema(description = "Whether the value falls outside its reference interval.")
                boolean isOutOfRange,
        @Nullable
                @Schema(
                        description =
                                "Id of the reference interval the value was checked "
                                        + "against, if any.")
                ReferenceIntervalId referenceIntervalId,
        @Schema(description = "Current lifecycle state of the result (e.g. draft, validated).")
                ResultState resultState,
        @Nullable
                @Schema(
                        description =
                                "When the result was biologically validated, if it has " + "been.")
                Instant biologicalValidatedAt,
        @Schema(description = "When this line was created.") Instant createdAt) {}
