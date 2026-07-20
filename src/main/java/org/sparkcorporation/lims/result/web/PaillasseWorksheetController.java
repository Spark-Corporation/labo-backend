package org.sparkcorporation.lims.result.web;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.result.infra.pdf.PaillasseWorksheetModelAssembler;
import org.sparkcorporation.lims.result.infra.pdf.PaillasseWorksheetPdfDocument;

/** CDC §VII — per-paillasse worklist PDF. from/to default to today (UTC), registration date. */
@RestController
@RequestMapping("/api/v1/paillasses")
@RequiredArgsConstructor
public class PaillasseWorksheetController {

    private final PaillasseWorksheetModelAssembler assembler;
    private final PaillasseWorksheetPdfDocument docType;
    private final PdfRenderingService pdfService;
    private final Clock clock;

    @GetMapping("/{disciplineId}/worksheet.pdf")
    @RequiresPermission("lab-file.read")
    public ResponseEntity<byte[]> get(
            @PathVariable String disciplineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable LocalDate to) {
        var today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        var effectiveFrom = from == null ? today : from;
        var effectiveTo = to == null ? today : to;
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        var model = assembler.assemble(new DisciplineId(disciplineId), effectiveFrom, effectiveTo);
        var rendered = pdfService.render(docType, model);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + rendered.suggestedFilename() + "\"")
                .body(rendered.bytes());
    }
}
