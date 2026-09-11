package org.sparkcorporation.lims.patient.web;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

// Security-sensitive: never log or echo the portalPassword value.
@Schema(description = "Partial update to a patient; omitted (null) fields are left unchanged.")
public record UpdatePatientRequest(
        @Schema(description = "New name, if it should change.") @Valid
                CreatePatientRequest.@Nullable NameRequest name,
        @Schema(description = "New civility/title, if it should change.")
                @Nullable Civility civility,
        @Schema(description = "New administrative sex, if it should change.") @Nullable Sex sex,
        @Schema(description = "New date of birth, if it should change; must not be in the future.")
                @PastOrPresent
                @Nullable LocalDate birthDate,
        @Schema(description = "New identity document, if it should change.") @Valid
                CreatePatientRequest.@Nullable IdDocumentRequest idDocument,
        @Schema(description = "New profession, if it should change.") @Size(max = 80)
                @Nullable String profession,
        @Schema(description = "New postal address, if it should change.") @Size(max = 255)
                @Nullable String address,
        @Schema(description = "New free-text clinical notes, if they should change.")
                @Nullable String clinicalNotes,
        @Schema(description = "New contact details, if they should change.") @Valid
                CreatePatientRequest.@Nullable ContactRequest contact,
        @Schema(description = "New guardian details, if they should change.") @Valid
                CreatePatientRequest.@Nullable GuardianRequest guardian,
        @Schema(description = "New insurance ULID, if it should change.") @Size(min = 26, max = 26)
                @Nullable String insuranceId,
        @Schema(description = "New partner lab ULID, if it should change.")
                @Size(min = 26, max = 26)
                @Nullable String partnerId,
        @Schema(description = "New patient portal login, if it should change.")
                @Size(min = 4, max = 80)
                @Nullable String portalLogin,
        @Schema(
                        description =
                                "New patient portal password, if it should change; must be 8-100"
                                        + " characters.")
                @Size(min = 8, max = 100)
                @Nullable String portalPassword) {}
