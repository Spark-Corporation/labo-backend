package org.sparkcorporation.lims.patient.web;

import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.patient.domain.GuardianRelationship;
import org.sparkcorporation.lims.patient.domain.IdDocumentType;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

public record PatientResponse(
        String id,
        String labCode,
        Name name,
        Civility civility,
        Sex sex,
        @Nullable LocalDate birthDate,
        @Nullable IdDoc idDocument,
        @Nullable String profession,
        @Nullable String address,
        @Nullable String clinicalNotes,
        Contact contact,
        @Nullable Guardian guardian,
        @Nullable String insuranceId,
        @Nullable String partnerId,
        @Nullable String portalLogin,
        @Nullable String mergedIntoId,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt,
        int version) {

    public record Name(String lastName, @Nullable String firstNames, @Nullable String maidenName) {}

    public record IdDoc(IdDocumentType type, String number) {}

    public record Contact(@Nullable String phone, @Nullable String email) {}

    public record Guardian(
            String name, @Nullable String phone, GuardianRelationship relationship) {}
}
