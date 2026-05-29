package org.sparkcorporation.lims.order.api;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.iam.api.UserId;

/**
 * Public, write-side LabFile interface for cross-module consumers that legitimately need to mutate
 * a {@link org.sparkcorporation.lims.order.domain.LabFile}.
 *
 * <p>Kept separate from the read-only {@link LabFileApi} so that the read contract stays
 * inspectable in isolation. The only consumer today is the {@code cashbox} module (1.7.a, home-
 * collection fee path).
 */
public interface LabFileWriteApi {

    /**
     * Updates the home-collection fee on a lab file. {@code recordedBy} is accepted for future
     * audit-trail use; the underlying {@code LabFile.update(UpdateLabFile, Clock)} write does not
     * yet persist a per-update user (only {@code updated_at}). When the audit trail lands as a
     * separate sub-project, this parameter will start flowing into the audit log without an API
     * change here.
     *
     * @throws org.sparkcorporation.lims.order.domain.LabFileNotEditableException if the lab file's
     *     status is not {@code OPEN}
     * @throws org.sparkcorporation.lims.order.domain.LabFileNotFoundException if no lab file
     *     matches the given id
     */
    void setHomeCollectionFee(LabFileId labFileId, BigDecimal amount, UserId recordedBy);

    /**
     * Transitions the lab file to PUBLISHED (idempotent). Eligibility is enforced by the caller.
     */
    void publish(LabFileId labFileId);

    /**
     * Transitions the lab file to SIGNED (idempotent), recording {@code signedBy} and the signing
     * instant. Eligibility ("all results biologically validated") and the cryptographic signature
     * are enforced by the caller ({@code delivery} module).
     */
    void markSigned(LabFileId labFileId, @Nullable String signedBy);

    /**
     * Stamps {@code emailed_at} on the lab file (idempotent — the latest delivery instant wins),
     * recording that the published dossier was successfully emailed to the patient (CDC §IV). The
     * only consumer is {@code delivery}'s {@code NotificationDelivered} listener, which runs after
     * the EMAIL notification has actually been dispatched.
     *
     * @throws org.sparkcorporation.lims.order.domain.LabFileNotFoundException if no lab file
     *     matches the given id
     */
    void markEmailed(LabFileId labFileId, Instant emailedAt);
}
