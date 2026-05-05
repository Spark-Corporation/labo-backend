package org.sparkcorporation.lims.patient.web;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

public record UpdatePatientRequest(
        @Valid CreatePatientRequest.@Nullable NameRequest name,
        @Nullable Civility civility,
        @Nullable Sex sex,
        @PastOrPresent @Nullable LocalDate birthDate,
        @Valid CreatePatientRequest.@Nullable IdDocumentRequest idDocument,
        @Size(max = 80) @Nullable String profession,
        @Size(max = 255) @Nullable String address,
        @Nullable String clinicalNotes,
        @Valid CreatePatientRequest.@Nullable ContactRequest contact,
        @Valid CreatePatientRequest.@Nullable GuardianRequest guardian,
        @Nullable Boolean insured,
        @Size(min = 4, max = 80) @Nullable String portalLogin,
        @Size(min = 8, max = 100) @Nullable String portalPassword) {}
