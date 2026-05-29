package org.sparkcorporation.lims.delivery.domain;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Raised when a published dossier is sent but the patient has no email address on file — the result
 * dossier is delivered only by email (the password travels out-of-band). Mapped to HTTP 422 by
 * {@code DeliveryProblemDetailAdvice}.
 *
 * <p>Owned by {@code delivery}; sibling to {@code order}'s {@code
 * MissingRecipientContactException}. The two modules do not share this type.
 */
public class MissingPatientEmailException extends RuntimeException {

    private final transient LabFileId labFileId;

    public MissingPatientEmailException(LabFileId labFileId) {
        super("Lab file " + labFileId.value() + " patient has no email address on file");
        this.labFileId = labFileId;
    }

    public LabFileId labFileId() {
        return labFileId;
    }
}
