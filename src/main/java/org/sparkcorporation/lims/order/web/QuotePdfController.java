package org.sparkcorporation.lims.order.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportModelAssembler;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportPdfDocument;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;

/**
 * Synchronous quote (devis) PDF endpoint (spec 1.5.h).
 *
 * <p>{@code GET /api/v1/quotes/{id}/pdf} → A4 {@code application/pdf} bytes as an attachment.
 * Returns 404 when the id is unknown (propagated via the shared 404 advice since {@code
 * QuoteNotFoundException extends EntityNotFoundException}).
 *
 * <p>Authorization: {@code quote.read}.
 */
@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuotePdfController {

    private final QuoteReportModelAssembler assembler;
    private final QuoteReportPdfDocument docType;
    private final PdfRenderingService pdfService;

    @GetMapping("/{id}/pdf")
    @RequiresPermission("quote.read")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        var model = assembler.assemble(new QuoteId(id));
        var rendered = pdfService.render(docType, model);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + rendered.suggestedFilename() + "\"")
                .body(rendered.bytes());
    }
}
