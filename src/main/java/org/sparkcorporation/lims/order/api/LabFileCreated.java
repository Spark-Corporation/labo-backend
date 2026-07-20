package org.sparkcorporation.lims.order.api;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.patient.api.PatientId;

/** Domain event — fired after a new LabFile is persisted. */
public record LabFileCreated(
        LabFileId labFileId,
        PatientId patientId,
        @Nullable String prescriberId,
        LabFileRegistrationNumber registrationNumber,
        boolean urgent,
        Instant occurredAt) {}
