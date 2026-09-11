package org.sparkcorporation.lims.order.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.infra.pdf.WorksheetModelAssembler;
import org.sparkcorporation.lims.order.infra.pdf.WorksheetPdfDocument;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;

/**
 * CDC §II « feuille de paillasse » — {@code GET /api/v1/lab-files/{id}/worksheet.pdf}, synchronous
 * render. Returns 404 when the id is unknown or soft-deleted (propagated via the shared 404 advice
 * since {@code LabFileNotFoundException extends EntityNotFoundException}).
 *
 * <p>The PDF carries patient identifying data: never log the body (controller logs nothing; ULIDs
 * only via MDC).
 *
 * <p>Authorization: {@code lab-file.read}.
 */
@RestController
@RequestMapping("/api/v1/lab-files")
@RequiredArgsConstructor
public class WorksheetPdfController {

    private final WorksheetModelAssembler assembler;
    private final WorksheetPdfDocument docType;
    private final PdfRenderingService pdfService;

    @GetMapping("/{id}/worksheet.pdf")
    @Operation(
            summary = "Get a lab file's worksheet as PDF",
            description =
                    "Synchronously renders the dossier's bench worksheet (feuille de paillasse, CDC"
                            + " §II) as a PDF and streams it as an attachment. Returns 404 when the"
                            + " lab file id is unknown or soft-deleted.")
    @RequiresPermission("lab-file.read")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        var model = assembler.assemble(new LabFileId(id));
        var rendered = pdfService.render(docType, model);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + rendered.suggestedFilename() + "\"")
                .body(rendered.bytes());
    }
}
