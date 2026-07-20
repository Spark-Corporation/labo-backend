package org.sparkcorporation.lims.order.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.sparkcorporation.lims.attachment.api.AttachableType;
import org.sparkcorporation.lims.attachment.api.AttachmentApi;
import org.sparkcorporation.lims.attachment.api.AttachmentId;
import org.sparkcorporation.lims.attachment.api.AttachmentSummary;
import org.sparkcorporation.lims.attachment.api.AttachmentUpload;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;

/**
 * Lab-file (dossier) attachments — CDC §IV "joindre des fichiers par scan direct" (SP6).
 * Nested-resource topology so {@code @RequiresPermission} stays a compile-time constant (spec §3).
 * Write ops reuse {@code lab-file.create}; reads reuse {@code lab-file.read} (spec §4).
 */
@RestController
@RequestMapping("/api/v1/lab-files/{labFileId}/attachments")
@RequiredArgsConstructor
public class LabFileAttachmentController {

    private final LabFileApi labFileApi;
    private final AttachmentApi attachmentApi;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("lab-file.create")
    public AttachmentSummary upload(
            @PathVariable String labFileId, @RequestParam("file") MultipartFile file) {
        requireLabFile(labFileId);
        return attachmentApi.upload(
                new AttachmentUpload(
                        AttachableType.LAB_FILE,
                        labFileId,
                        sanitizeFilename(file.getOriginalFilename()),
                        file.getContentType(),
                        bytesOf(file),
                        callerId()));
    }

    @GetMapping
    @RequiresPermission("lab-file.read")
    public List<AttachmentSummary> list(@PathVariable String labFileId) {
        requireLabFile(labFileId);
        return attachmentApi.list(AttachableType.LAB_FILE, labFileId);
    }

    @GetMapping("/{attachmentId}")
    @RequiresPermission("lab-file.read")
    public ResponseEntity<byte[]> download(
            @PathVariable String labFileId, @PathVariable String attachmentId) {
        requireLabFile(labFileId);
        var rendered = attachmentApi.get(new AttachmentId(attachmentId));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rendered.contentType()))
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + sanitizeFilename(rendered.filename()) + "\"")
                .body(rendered.bytes());
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("lab-file.create")
    public void softDelete(@PathVariable String labFileId, @PathVariable String attachmentId) {
        requireLabFile(labFileId);
        attachmentApi.softDelete(new AttachmentId(attachmentId));
    }

    private void requireLabFile(String labFileId) {
        var id = new LabFileId(labFileId);
        if (!labFileApi.existsById(id)) {
            throw new LabFileNotFoundException(id);
        }
    }

    private static byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read multipart upload", e);
        }
    }

    /** Header/PII hygiene: strip CR/LF/quotes before echoing a client filename into a header. */
    private static String sanitizeFilename(String filename) {
        var safe = filename == null || filename.isBlank() ? "attachment" : filename;
        return safe.replaceAll("[\\r\\n\"\\\\]", "_");
    }

    private static String callerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("no authenticated user in SecurityContext");
        }
        return auth.getName();
    }
}
