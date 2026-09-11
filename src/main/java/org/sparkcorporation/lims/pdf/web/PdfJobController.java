package org.sparkcorporation.lims.pdf.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.sparkcorporation.lims.pdf.api.PdfJobNotFoundException;
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
    @Operation(
            summary = "Poll or download an async PDF render job",
            description =
                    "By default returns the job's status row (QUEUED, RUNNING, DONE or FAILED) with"
                        + " the PDF bytes stripped, for polling. With download=true, streams the"
                        + " rendered PDF bytes once the job is DONE. Returns 409 when the job is"
                        + " not yet DONE, 410 when it FAILED, and 403 when the caller is not the"
                        + " original requester of a job that has a recorded owner.")
    @RequiresPermission("pdf.read-own-jobs")
    public ResponseEntity<?> view(
            @PathVariable String id,
            @Parameter(
                            description =
                                    "When true, streams the rendered PDF bytes instead of the job"
                                            + " status row; only valid once the job is DONE.")
                    @RequestParam(value = "download", defaultValue = "false")
                    boolean download) {

        var record =
                renderingService
                        .findJobRecord(id)
                        .orElseThrow(() -> new PdfJobNotFoundException(id));

        var caller = currentAuthName();
        if (record.requestedByUserId() != null
                && caller != null
                && !record.requestedByUserId().equals(caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not the job requester");
        }

        if (!download) {
            return ResponseEntity.ok(record.toView());
        }

        return switch (record.status()) {
            case QUEUED, RUNNING ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "pdf job not ready");
            case FAILED ->
                    throw new ResponseStatusException(
                            HttpStatus.GONE, "pdf job failed: " + record.errorMessage());
            case DONE ->
                    ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(
                                    "Content-Disposition",
                                    "attachment; filename=\"" + record.suggestedFilename() + "\"")
                            .body(record.bytes());
        };
    }

    private static @Nullable String currentAuthName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null
                        && auth.isAuthenticated()
                        && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName()
                : null;
    }
}
