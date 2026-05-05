package org.sparkcorporation.lims.patient.domain;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

/** Command for creating a {@link Patient}. All required CDC §I fields, optional fields nullable. */
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
        boolean insured,
        @Nullable String portalLogin,
        @Nullable String portalPasswordHash) {}
