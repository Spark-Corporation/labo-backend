package org.sparkcorporation.lims.result.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportModelAssembler;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportPdfDocument;

/**
 * Synchronous CR (compte-rendu) PDF endpoint — first end-to-end consumer of the {@code pdf} module
 * (spec §6.4).
 *
 * <p>{@code GET /api/v1/results/{labFileId}/pdf} → {@code application/pdf} bytes inline. Returns
 * 409 {@code /errors/result/results.not-validated} when any result on the dossier is not yet
 * biologically validated; 404 when the lab file is unknown (propagated via shared advice).
 */
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultPdfController {

    private final ResultReportModelAssembler assembler;
    private final ResultReportPdfDocument resultReportPdfDocument;
    private final PdfRenderingService pdfService;

    @GetMapping("/{labFileId}/pdf")
    @RequiresPermission("lab-file.read")
    public ResponseEntity<byte[]> getResultCrPdf(@PathVariable String labFileId) {
        var model = assembler.assemble(new LabFileId(labFileId));
        var rendered = pdfService.render(resultReportPdfDocument, model);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + rendered.suggestedFilename() + "\"")
                .body(rendered.bytes());
    }
}
