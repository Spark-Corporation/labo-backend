package org.sparkcorporation.lims.result.api;

import java.time.Instant;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Cross-module read shape for the per-{@code LabFile} dossier-wide comment block (CDC §IV "Section
 * Commentaires généraux"). 1:1 with {@code LabFile}; absent when never created.
 */
public record DossierCommentSummary(
        LabFileId labFileId, String commentText, Instant createdAt, Instant updatedAt) {}
