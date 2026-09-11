package org.sparkcorporation.lims.cashbox.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import org.sparkcorporation.lims.attachment.api.AttachmentId;
import org.sparkcorporation.lims.attachment.api.AttachmentSummary;
import org.sparkcorporation.lims.iam.api.UserId;

/**
 * Expense justification scans (tech-doc §8.6, SP6 spec §4). Write ops reuse {@code
 * cashbox.expense.record}; reads reuse {@code cashbox.expense.read}.
 */
@RestController
@RequestMapping("/api/v1/cashbox/expenses/{expenseId}/attachments")
@RequiredArgsConstructor
public class ExpenseAttachmentController {

    private final ExpenseAttachmentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("cashbox.expense.record")
    @Operation(
            summary = "Upload an expense justification scan",
            description =
                    "Uploads a scanned justification document and links it to the expense as its"
                            + " justification-scan pointer. Re-uploading replaces the pointer; the"
                            + " prior attachment row is kept independently.")
    public AttachmentSummary upload(
            @PathVariable @Parameter(description = "ULID of the expense") String expenseId,
            @RequestParam("file") MultipartFile file) {
        var scan =
                new UploadedScan(
                        sanitizeFilename(file.getOriginalFilename()),
                        file.getContentType(),
                        bytesOf(file));
        return service.uploadAndLink(expenseId, scan, callerId());
    }

    @GetMapping
    @RequiresPermission("cashbox.expense.read")
    @Operation(
            summary = "List an expense's justification scans",
            description = "Returns every attachment uploaded against this expense.")
    public List<AttachmentSummary> list(
            @PathVariable @Parameter(description = "ULID of the expense") String expenseId) {
        return service.list(expenseId);
    }

    @GetMapping("/{attachmentId}")
    @RequiresPermission("cashbox.expense.read")
    @Operation(
            summary = "Download an expense justification scan",
            description = "Streams the raw bytes of one previously uploaded attachment.")
    public ResponseEntity<byte[]> download(
            @PathVariable @Parameter(description = "ULID of the expense") String expenseId,
            @PathVariable @Parameter(description = "ULID of the attachment to download")
                    String attachmentId) {
        var rendered = service.download(expenseId, new AttachmentId(attachmentId));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rendered.contentType()))
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + sanitizeFilename(rendered.filename()) + "\"")
                .body(rendered.bytes());
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("cashbox.expense.record")
    @Operation(
            summary = "Remove an expense justification scan",
            description = "Soft-deletes one previously uploaded attachment.")
    public void softDelete(
            @PathVariable @Parameter(description = "ULID of the expense") String expenseId,
            @PathVariable @Parameter(description = "ULID of the attachment to remove")
                    String attachmentId) {
        service.softDelete(expenseId, new AttachmentId(attachmentId));
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

    private static UserId callerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("no authenticated user in SecurityContext");
        }
        return new UserId(auth.getName());
    }
}
