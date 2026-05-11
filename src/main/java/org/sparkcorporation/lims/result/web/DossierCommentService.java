package org.sparkcorporation.lims.result.web;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.DossierCommentSummary;
import org.sparkcorporation.lims.result.domain.DossierComment;
import org.sparkcorporation.lims.result.domain.DossierCommentNotFoundException;
import org.sparkcorporation.lims.result.infra.DossierCommentRepository;

/**
 * Application service for {@link DossierComment}. Per spec §4.4 the comment-block lifecycle is
 * independent of {@code Result}: read / upsert / hard-delete keyed on {@code labFileId}. {@code
 * upsert} verifies the parent {@code LabFile} exists via {@link LabFileApi#requireById} so the
 * shared advice's 404 mapper is reused for missing-parent rejection.
 */
@Service
@RequiredArgsConstructor
public class DossierCommentService {

    private final DossierCommentRepository repo;
    private final LabFileApi labFileApi;

    @Transactional(readOnly = true)
    public Optional<DossierCommentSummary> get(LabFileId labFileId) {
        return repo.findById(labFileId.value()).map(this::toSummary);
    }

    @Transactional
    public DossierCommentSummary upsert(LabFileId labFileId, UpsertDossierCommentRequest req) {
        labFileApi.requireById(labFileId); // 404 propagates if LabFile missing
        DossierComment dc =
                repo.findById(labFileId.value())
                        .orElseGet(() -> DossierComment.create(labFileId, req.commentText()));
        dc.update(req.commentText());
        repo.save(dc);
        return toSummary(dc);
    }

    @Transactional
    public void delete(LabFileId labFileId) {
        DossierComment dc =
                repo.findById(labFileId.value())
                        .orElseThrow(() -> new DossierCommentNotFoundException(labFileId));
        repo.delete(dc);
    }

    private DossierCommentSummary toSummary(DossierComment dc) {
        return new DossierCommentSummary(
                new LabFileId(dc.getLabFileId()),
                dc.getCommentText(),
                dc.getCreatedAt(),
                dc.getUpdatedAt());
    }
}
