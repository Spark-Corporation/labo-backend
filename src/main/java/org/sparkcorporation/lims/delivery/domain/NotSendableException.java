package org.sparkcorporation.lims.delivery.domain;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Raised when {@code :send} is invoked on a dossier whose status is not {@code PUBLISHED}: only a
 * published dossier carries biologist-validated, signed-off results that may be delivered to the
 * patient (CDC §IV). Mapped to HTTP 409 by {@code DeliveryProblemDetailAdvice}.
 *
 * <p>Owned by {@code delivery}; the {@code order} module defines its own sibling for the quote
 * {@code :send} path. The two modules do not share this type.
 */
public class NotSendableException extends RuntimeException {

    private final transient LabFileId labFileId;

    public NotSendableException(LabFileId labFileId) {
        super("Lab file " + labFileId.value() + " is not PUBLISHED and cannot be sent");
        this.labFileId = labFileId;
    }

    public LabFileId labFileId() {
        return labFileId;
    }
}
