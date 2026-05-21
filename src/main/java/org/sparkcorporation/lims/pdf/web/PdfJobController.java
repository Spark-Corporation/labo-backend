package org.sparkcorporation.lims.pdf.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.sparkcorporation.lims.pdf.api.PdfJobNotFoundException;
import org.sparkcorporation.lims.pdf.api.PdfRenderJobView;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;

/**
 * Polling endpoint for PDF render jobs. By default returns the status row (bytes stripped); with
 * {@code ?download=true} streams the PDF bytes as {@code application/pdf} when status is {@code
 * DONE}, 409 when not yet ready, 410 when failed.
 *
 * <p>Authorization: caller must hold {@code pdf.read-own-jobs} AND be the original requester of the
 * job (or the job must be system-issued, i.e. no owner recorded).
 */
@RestController
@RequestMapping("/api/v1/pdf-jobs")
@RequiredArgsConstructor
public class PdfJobController {

    private final PdfRenderingService renderingService;

    @GetMapping("/{id}")
    @RequiresPermission("pdf.read-own-jobs")
    public ResponseEntity<?> view(
            @PathVariable String id,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {

        var view = renderingService.findJob(id).orElseThrow(() -> new PdfJobNotFoundException(id));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String caller =
                (auth != null
                                && auth.isAuthenticated()
                                && !"anonymousUser".equals(auth.getPrincipal()))
                        ? auth.getName()
                        : null;
        var ownerUserId = renderingService.findJobOwner(id).orElse(null);
        if (ownerUserId != null && caller != null && !ownerUserId.equals(caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not the job requester");
        }

        if (!download) {
            var stripped =
                    new PdfRenderJobView(
                            view.id(),
                            view.documentTypeId(),
                            view.status(),
                            null,
                            view.suggestedFilename(),
                            view.errorMessage(),
                            view.requestedAt(),
                            view.completedAt());
            return ResponseEntity.ok(stripped);
        }

        return switch (view.status()) {
            case QUEUED, RUNNING ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "pdf job not ready");
            case FAILED ->
                    throw new ResponseStatusException(
                            HttpStatus.GONE, "pdf job failed: " + view.errorMessage());
            case DONE ->
                    ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(
                                    "Content-Disposition",
                                    "attachment; filename=\"" + view.suggestedFilename() + "\"")
                            .body(view.bytes());
        };
    }
}
