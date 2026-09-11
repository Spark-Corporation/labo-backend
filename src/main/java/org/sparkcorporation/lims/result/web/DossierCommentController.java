package org.sparkcorporation.lims.result.web;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Admin REST controller for the per-{@code LabFile} dossier comment-block per spec §6.3. 3
 * endpoints under {@code /api/v1/lab-files/{labFileId}/dossier-comment}, all gated by {@code
 * result.enter}. The 1:1 row uses PUT for upsert (idempotent), GET returns 200 or 204, DELETE
 * returns 204 (or 404 if missing).
 */
@RestController
@RequestMapping("/api/v1/lab-files/{labFileId}/dossier-comment")
@RequiredArgsConstructor
public class DossierCommentController {

    private final DossierCommentService service;

    @GetMapping
    @RequiresPermission("result.enter")
    @Operation(
            summary = "Get the dossier's general comment",
            description =
                    "Returns the single general comment attached to the lab file's dossier, or 204"
                            + " No Content if none has been recorded yet.")
    public ResponseEntity<DossierCommentResponse> get(@PathVariable String labFileId) {
        return service.get(new LabFileId(labFileId))
                .map(DossierCommentMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    @RequiresPermission("result.enter")
    @Operation(
            summary = "Create or replace the dossier's general comment",
            description =
                    "Idempotent upsert of the single general-comment row attached 1:1 to the lab"
                            + " file's dossier. Creates the row on first call, replaces its text on"
                            + " subsequent calls.")
    public DossierCommentResponse upsert(
            @PathVariable String labFileId, @Valid @RequestBody UpsertDossierCommentRequest req) {
        return DossierCommentMapper.toResponse(service.upsert(new LabFileId(labFileId), req));
    }

    @DeleteMapping
    @RequiresPermission("result.enter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete the dossier's general comment",
            description =
                    "Removes the general-comment row attached to the lab file's dossier, if one"
                            + " exists. Returns 204 whether or not a comment was present.")
    public void delete(@PathVariable String labFileId) {
        service.delete(new LabFileId(labFileId));
    }
}
