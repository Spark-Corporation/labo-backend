package org.sparkcorporation.lims.stats.web;

import java.time.LocalDate;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.export.api.OfficeRenderingService;
import org.sparkcorporation.lims.export.api.RenderedOffice;
import org.sparkcorporation.lims.export.api.UnsupportedExportFormatException;
import org.sparkcorporation.lims.stats.infra.export.StatsWorkbookAssembler;
import org.sparkcorporation.lims.stats.infra.export.StatsWorkbookDocument;

/**
 * Statistics XLSX workbook export (CDC §Onglet Statistiques / SP5). {@code GET
 * /api/v1/stats/export?format=xlsx&from=&to=}. No password — statistics exports are not
 * patient-sensitive documents (YAGNI per spec §3/§10).
 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsExportController {

    private final StatsWorkbookAssembler assembler;
    private final StatsWorkbookDocument docType;
    private final OfficeRenderingService renderingService;

    @GetMapping("/export")
    @RequiresPermission("stats.read")
    public ResponseEntity<byte[]> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam String format) {
        if (!"xlsx".equalsIgnoreCase(format)) {
            throw new UnsupportedExportFormatException(format, "xlsx");
        }
        var model = assembler.assemble(from, to);
        RenderedOffice rendered = renderingService.render(docType, model);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rendered.format().mimeType()))
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + rendered.suggestedFilename() + "\"")
                .body(rendered.bytes());
    }
}
