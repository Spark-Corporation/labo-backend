package org.sparkcorporation.lims.delivery.domain;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Raised when an electronic signature is attempted on a dossier whose results are not all
 * biologically validated. Mapped to HTTP 409 by {@code DeliveryProblemDetailAdvice}.
 */
public class NotSignableException extends RuntimeException {

    private final transient LabFileId labFileId;

    public NotSignableException(LabFileId labFileId) {
        super(
                "Lab file "
                        + labFileId.value()
                        + " has results that are not all biologically validated");
        this.labFileId = labFileId;
    }

    public LabFileId labFileId() {
        return labFileId;
    }
}
