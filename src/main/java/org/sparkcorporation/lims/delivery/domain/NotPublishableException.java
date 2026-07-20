package org.sparkcorporation.lims.delivery.domain;

import org.sparkcorporation.lims.order.api.LabFileId;

/** Raised when a publish is attempted on a dossier whose results are not all bio-validated. */
public class NotPublishableException extends RuntimeException {

    private final transient LabFileId labFileId;

    public NotPublishableException(LabFileId labFileId) {
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
