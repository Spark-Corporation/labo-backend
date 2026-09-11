package org.sparkcorporation.lims.patient.web;

import java.time.Instant;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.patient.domain.GuardianRelationship;
import org.sparkcorporation.lims.patient.domain.IdDocumentType;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

@Schema(description = "A patient record.")
public record PatientResponse(
        @Schema(description = "ULID identifying the patient.") String id,
        @Schema(description = "Lab-assigned code for the patient, generated at creation.")
                String labCode,
        @Schema(description = "Patient's name.") Name name,
        @Schema(description = "Patient's civility/title.") Civility civility,
        @Schema(description = "Patient's administrative sex.") Sex sex,
        @Schema(description = "Patient's date of birth, when known.") @Nullable LocalDate birthDate,
        @Schema(description = "Patient's identity document, when known.")
                @Nullable IdDoc idDocument,
        @Schema(description = "Patient's profession.") @Nullable String profession,
        @Schema(description = "Patient's postal address.") @Nullable String address,
        @Schema(description = "Free-text clinical notes about the patient.")
                @Nullable String clinicalNotes,
        @Schema(description = "Patient's contact details.") Contact contact,
        @Schema(description = "Legal guardian details, for a minor or dependent patient.")
                @Nullable Guardian guardian,
        @Schema(description = "ULID of the insurance covering the patient, when applicable.")
                @Nullable String insuranceId,
        @Schema(description = "ULID of the referring partner lab, when applicable.")
                @Nullable String partnerId,
        @Schema(description = "Login used on the patient portal, when portal access is granted.")
                @Nullable String portalLogin,
        @Schema(
                        description =
                                "ULID of the surviving patient this record was merged into, if this"
                                        + " patient has been merged; null otherwise.")
                @Nullable String mergedIntoId,
        @Schema(description = "Whether this patient record has been soft-deleted.") boolean deleted,
        @Schema(description = "Instant the patient record was created.") Instant createdAt,
        @Schema(description = "Instant the patient record was last updated.") Instant updatedAt,
        @Schema(description = "Optimistic-locking version of the patient record.") int version) {

    @Schema(description = "A patient's name.")
    public record Name(
            @Schema(description = "Family name.") String lastName,
            @Schema(description = "Given name(s).") @Nullable String firstNames,
            @Schema(description = "Maiden (birth) name, when different from the family name.")
                    @Nullable String maidenName) {}

    @Schema(description = "A patient's identity document.")
    public record IdDoc(
            @Schema(description = "Type of identity document presented.") IdDocumentType type,
            @Schema(description = "Document number/identifier.") String number) {}

    @Schema(description = "A patient's contact details.")
    public record Contact(
            @Schema(description = "Phone number.") @Nullable String phone,
            @Schema(description = "Email address.") @Nullable String email) {}

    @Schema(description = "A patient's legal guardian, for a minor or dependent patient.")
    public record Guardian(
            @Schema(description = "Guardian's full name.") String name,
            @Schema(description = "Guardian's phone number.") @Nullable String phone,
            @Schema(description = "Guardian's relationship to the patient.")
                    GuardianRelationship relationship) {}
}
