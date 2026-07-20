package org.sparkcorporation.lims.delivery.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.delivery.infra.MarkPrintedService;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Staff print-confirmation — {@code POST /api/v1/lab-files/{id}:mark-printed}. Guarded by {@code
 * dossier.print} (ADMIN, BIOLOGIST, RECEPTIONIST). 409 when not PUBLISHED.
 */
@RestController
@RequestMapping("/api/v1/lab-files")
@RequiredArgsConstructor
public class MarkPrintedController {

    private final MarkPrintedService markPrintedService;

    @PostMapping("/{id}:mark-printed")
    @RequiresPermission("dossier.print")
    public ResponseEntity<Void> markPrinted(@PathVariable String id) {
        markPrintedService.markPrinted(new LabFileId(id));
        return ResponseEntity.noContent().build();
    }
}
