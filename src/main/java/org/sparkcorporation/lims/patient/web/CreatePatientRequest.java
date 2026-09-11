package org.sparkcorporation.lims.patient.web;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

// Security-sensitive: never log or echo the portalPassword value.
@Schema(description = "Request to create a new patient record.")
public record CreatePatientRequest(
        @Schema(description = "Patient's name.") @NotNull @Valid NameRequest name,
        @Schema(description = "Patient's civility/title.") @NotNull Civility civility,
        @Schema(description = "Patient's administrative sex.") @NotNull Sex sex,
        @Schema(description = "Patient's date of birth; must not be in the future.") @PastOrPresent
                @Nullable LocalDate birthDate,
        @Schema(description = "Patient's identity document, when known.") @Valid
                @Nullable IdDocumentRequest idDocument,
        @Schema(description = "Patient's profession.") @Size(max = 80) @Nullable String profession,
        @Schema(description = "Patient's postal address.") @Size(max = 255)
                @Nullable String address,
        @Schema(description = "Free-text clinical notes about the patient.")
                @Nullable String clinicalNotes,
        @Schema(description = "Patient's contact details.") @Valid @NotNull ContactRequest contact,
        @Schema(description = "Legal guardian details, for a minor or dependent patient.") @Valid
                @Nullable GuardianRequest guardian,
        @Schema(description = "ULID of the insurance covering the patient, when applicable.")
                @Size(min = 26, max = 26)
                @Nullable String insuranceId,
        @Schema(description = "ULID of the referring partner lab, when applicable.")
                @Size(min = 26, max = 26)
                @Nullable String partnerId,
        @Schema(
                        description =
                                "Login the patient will use on the patient portal, when portal"
                                        + " access is granted.")
                @Size(min = 4, max = 80)
                @Nullable String portalLogin,
        @Schema(
                        description =
                                "Initial password for the patient portal account; must be 8-100"
                                        + " characters.")
                @Size(min = 8, max = 100)
                @Nullable String portalPassword) {

    @Schema(description = "A patient's name.")
    public record NameRequest(
            @Schema(description = "Family name.") @NotBlank @Size(max = 120) String lastName,
            @Schema(description = "Given name(s).") @Size(max = 160) @Nullable String firstNames,
            @Schema(description = "Maiden (birth) name, when different from the family name.")
                    @Size(max = 120)
                    @Nullable String maidenName) {}

    @Schema(description = "A patient's identity document.")
    public record IdDocumentRequest(
            @Schema(description = "Type of identity document presented.") @NotNull
                    org.sparkcorporation.lims.patient.domain.IdDocumentType type,
            @Schema(description = "Document number/identifier.") @NotBlank @Size(max = 40)
                    String number) {}

    @Schema(description = "A patient's contact details.")
    public record ContactRequest(
            @Schema(description = "Phone number.") @Pattern(regexp = "\\+?[0-9 -]{6,30}")
                    @Nullable String phone,
            @Schema(description = "Email address.") @Email @Size(max = 120)
                    @Nullable String email) {}

    @Schema(description = "A patient's legal guardian, for a minor or dependent patient.")
    public record GuardianRequest(
            @Schema(description = "Guardian's full name.") @NotBlank @Size(max = 120) String name,
            @Schema(description = "Guardian's phone number.") @Pattern(regexp = "\\+?[0-9 -]{6,30}")
                    @Nullable String phone,
            @Schema(description = "Guardian's relationship to the patient.") @NotNull
                    org.sparkcorporation.lims.patient.domain.GuardianRelationship relationship) {}
}
