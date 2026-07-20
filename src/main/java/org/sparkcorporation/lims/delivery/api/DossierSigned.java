package org.sparkcorporation.lims.delivery.api;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Domain event — fired after a dossier is electronically signed (cryptographic signing chain +
 * lifecycle transition to {@code SIGNED}). Carries the {@code contentSha256} digest of the signed
 * result content so downstream consumers can correlate the signature with the signed snapshot.
 */
public record DossierSigned(
        LabFileId labFileId,
        PatientId patientId,
        @Nullable String signedBy,
        String contentSha256,
        Instant occurredAt) {}
