package org.sparkcorporation.lims.patient.web;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

public record CreatePatientRequest(
        @NotNull @Valid NameRequest name,
        @NotNull Civility civility,
        @NotNull Sex sex,
        @PastOrPresent @Nullable LocalDate birthDate,
        @Valid @Nullable IdDocumentRequest idDocument,
        @Size(max = 80) @Nullable String profession,
        @Size(max = 255) @Nullable String address,
        @Nullable String clinicalNotes,
        @Valid @NotNull ContactRequest contact,
        @Valid @Nullable GuardianRequest guardian,
        @Size(min = 26, max = 26) @Nullable String insuranceId,
        @Size(min = 26, max = 26) @Nullable String partnerId,
        @Size(min = 4, max = 80) @Nullable String portalLogin,
        @Size(min = 8, max = 100) @Nullable String portalPassword) {

    public record NameRequest(
            @NotBlank @Size(max = 120) String lastName,
            @Size(max = 160) @Nullable String firstNames,
            @Size(max = 120) @Nullable String maidenName) {}

    public record IdDocumentRequest(
            @NotNull org.sparkcorporation.lims.patient.domain.IdDocumentType type,
            @NotBlank @Size(max = 40) String number) {}

    public record ContactRequest(
            @Pattern(regexp = "\\+?[0-9 -]{6,30}") @Nullable String phone,
            @Email @Size(max = 120) @Nullable String email) {}

    public record GuardianRequest(
            @NotBlank @Size(max = 120) String name,
            @Pattern(regexp = "\\+?[0-9 -]{6,30}") @Nullable String phone,
            @NotNull org.sparkcorporation.lims.patient.domain.GuardianRelationship relationship) {}
}
