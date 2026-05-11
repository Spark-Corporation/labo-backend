package org.sparkcorporation.lims.result.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Thrown when a {@code DossierComment} for a {@code LabFile} is missing. Routed to {@code 404} via
 * the shared advice {@link EntityNotFoundException} mapper.
 */
public class DossierCommentNotFoundException extends EntityNotFoundException {

    public DossierCommentNotFoundException(LabFileId labFileId) {
        super("Dossier comment for LabFile " + labFileId.value() + " not found");
    }
}
