package org.sparkcorporation.lims.delivery.api;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Domain event — fired after a dossier is published. Consumed downstream by SP4 (notifications).
 */
public record ResultPublished(
        LabFileId labFileId,
        PatientId patientId,
        @Nullable String publishedBy,
        Instant occurredAt) {}
