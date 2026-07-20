package org.sparkcorporation.lims.order.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.export.api.OfficeRenderingService;
import org.sparkcorporation.lims.export.api.RenderedOffice;
import org.sparkcorporation.lims.export.api.UnsupportedExportFormatException;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.infra.export.QuoteWordDocument;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportModelAssembler;

/** Quote (devis) Word export (CDC §III). {@code GET /api/v1/quotes/{id}/export?format=docx}. */
@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteExportController {

    private final QuoteReportModelAssembler assembler;
    private final QuoteWordDocument docType;
    private final OfficeRenderingService renderingService;

    @GetMapping("/{id}/export")
    @RequiresPermission("quote.read")
    public ResponseEntity<byte[]> export(@PathVariable String id, @RequestParam String format) {
        if (!"docx".equalsIgnoreCase(format)) {
            throw new UnsupportedExportFormatException(format, "docx");
        }
        var model = assembler.assemble(new QuoteId(id));
        RenderedOffice rendered = renderingService.render(docType, model);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rendered.format().mimeType()))
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + rendered.suggestedFilename() + "\"")
                .body(rendered.bytes());
    }
}
