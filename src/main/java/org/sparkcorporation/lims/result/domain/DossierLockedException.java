package org.sparkcorporation.lims.result.domain;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Raised when a result mutation is attempted on a dossier that is electronically {@code SIGNED} (or
 * already {@code PUBLISHED}). The signed content is cryptographically attested, so no further edits
 * are permitted. Mapped to HTTP 409 by {@code ResultProblemDetailAdvice}.
 */
public class DossierLockedException extends RuntimeException {

    private final transient LabFileId labFileId;

    public DossierLockedException(LabFileId labFileId) {
        super("Lab file " + labFileId.value() + " is signed/published and cannot be edited");
        this.labFileId = labFileId;
    }

    public LabFileId labFileId() {
        return labFileId;
    }
}
