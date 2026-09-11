package org.sparkcorporation.lims.result.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.export.api.ExportPasswordGenerator;
import org.sparkcorporation.lims.export.api.OfficeDocumentType;
import org.sparkcorporation.lims.export.api.OfficePasswordProtector;
import org.sparkcorporation.lims.export.api.OfficeRenderingService;
import org.sparkcorporation.lims.export.api.RenderedOffice;
import org.sparkcorporation.lims.export.api.UnsupportedExportFormatException;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.infra.export.DossierExcelDocument;
import org.sparkcorporation.lims.result.infra.export.DossierWordDocument;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportModel;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportModelAssembler;

/**
 * Result dossier office export (CDC §IV) — Word or Excel, password-protected. {@code GET
 * /api/v1/lab-files/{id}/export?format=docx|xlsx}. The generated password rides in the {@code
 * X-Export-Password} response header (never logged — see logging hygiene).
 */
@RestController
@RequestMapping("/api/v1/lab-files")
@RequiredArgsConstructor
public class DossierExportController {

    private final ResultReportModelAssembler assembler;
    private final DossierWordDocument wordDoc;
    private final DossierExcelDocument excelDoc;
    private final OfficeRenderingService renderingService;
    private final OfficePasswordProtector protector;
    private final ExportPasswordGenerator passwordGenerator;

    @GetMapping("/{id}/export")
    @RequiresPermission("lab-file.read")
    @Operation(
            summary = "Export a dossier as a password-protected office document",
            description =
                    "Renders the result dossier as a Word (docx) or Excel (xlsx) document and"
                        + " returns it password-protected. The generated password is carried in the"
                        + " X-Export-Password response header (never logged).")
    public ResponseEntity<byte[]> export(
            @PathVariable @Parameter(description = "ULID of the lab file to export") String id,
            @RequestParam @Parameter(description = "Output format: \"docx\" or \"xlsx\"")
                    String format) {
        OfficeDocumentType<ResultReportModel> docType = selectDocType(format);
        var model = assembler.assemble(new LabFileId(id));
        RenderedOffice rendered = renderingService.render(docType, model);
        String password = passwordGenerator.generate();
        byte[] encrypted = protector.protect(rendered.bytes(), password);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rendered.format().mimeType()))
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" + rendered.suggestedFilename() + "\"")
                .header("X-Export-Password", password)
                .body(encrypted);
    }

    private OfficeDocumentType<ResultReportModel> selectDocType(String format) {
        return switch (format.toLowerCase()) {
            case "docx" -> wordDoc;
            case "xlsx" -> excelDoc;
            default -> throw new UnsupportedExportFormatException(format, "docx or xlsx");
        };
    }
}
