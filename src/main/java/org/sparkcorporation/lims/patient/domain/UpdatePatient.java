package org.sparkcorporation.lims.patient.domain;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

/** Partial-update command. Null fields are ignored; non-null fields overwrite. */
public record UpdatePatient(
        @Nullable PersonName name,
        @Nullable Civility civility,
        @Nullable Sex sex,
        @Nullable LocalDate birthDate,
        @Nullable IdDocument idDocument,
        @Nullable String profession,
        @Nullable String address,
        @Nullable String clinicalNotes,
        @Nullable ContactInfo contact,
        @Nullable Guardian guardian,
        @Nullable Boolean insured,
        @Nullable String portalLogin,
        @Nullable String portalPasswordHash) {}
