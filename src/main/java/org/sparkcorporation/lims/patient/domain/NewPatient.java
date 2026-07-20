package org.sparkcorporation.lims.patient.domain;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Command for creating a {@link Patient}. All required CDC §I fields, optional fields nullable.
 *
 * <p>{@code insuranceId} / {@code partnerId} are nullable opaque ULID strings — the Patient entity
 * stays free of catalog typed-id imports. {@link
 * org.sparkcorporation.lims.patient.web.PatientService} validates the FKs through {@code
 * catalog::api} before invoking {@link Patient#create(NewPatient,
 * org.sparkcorporation.lims.patient.domain.LabCode)}.
 */
public record NewPatient(
        PersonName name,
        Civility civility,
        Sex sex,
        @Nullable LocalDate birthDate,
        @Nullable IdDocument idDocument,
        @Nullable String profession,
        @Nullable String address,
        @Nullable String clinicalNotes,
        ContactInfo contact,
        @Nullable Guardian guardian,
        @Nullable String insuranceId,
        @Nullable String partnerId,
        @Nullable String portalLogin,
        @Nullable String portalPasswordHash) {}
