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
import org.sparkcorporation.lims.result.api.CompteRenduApi;

/**
 * Synchronous CR (compte-rendu) PDF endpoint — first end-to-end consumer of the {@code pdf} module
 * (spec §6.4).
 *
 * <p>{@code GET /api/v1/results/{labFileId}/pdf} → {@code application/pdf} bytes inline. Returns
 * 409 {@code /errors/result/results.not-validated} when any result on the dossier is not yet
 * biologically validated; 404 when the lab file is unknown (propagated via shared advice).
 *
 * <p>Rendering is delegated to {@link CompteRenduApi} (owned by {@code result.api}) so the same
 * capability can be reused by {@code delivery} without it depending on the {@code pdf} module.
 */
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
public class ResultPdfController {

    private final CompteRenduApi compteRenduApi;

    @GetMapping("/{labFileId}/pdf")
    @RequiresPermission("lab-file.read")
    public ResponseEntity<byte[]> getResultCrPdf(@PathVariable String labFileId) {
        var cr = compteRenduApi.renderCompteRendu(new LabFileId(labFileId));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + cr.filename() + "\"")
                .body(cr.bytes());
    }
}
