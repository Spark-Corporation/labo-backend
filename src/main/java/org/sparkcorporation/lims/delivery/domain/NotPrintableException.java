package org.sparkcorporation.lims.delivery.domain;

import org.sparkcorporation.lims.order.api.LabFileId;

/** Thrown by {@code :mark-printed} when the dossier is not PUBLISHED. Maps to 409. */
public class NotPrintableException extends RuntimeException {

    private final transient LabFileId labFileId;

    public NotPrintableException(LabFileId labFileId) {
        super("Lab file " + labFileId.value() + " is not PUBLISHED and cannot be marked printed");
        this.labFileId = labFileId;
    }

    public LabFileId labFileId() {
        return labFileId;
    }
}
