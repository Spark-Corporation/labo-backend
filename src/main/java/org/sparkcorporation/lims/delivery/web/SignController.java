package org.sparkcorporation.lims.delivery.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.delivery.infra.SignDossierService;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Staff electronic-signature endpoint — {@code POST /api/v1/lab-files/{id}:sign}. Guarded by {@code
 * dossier.sign-electronically} (granted to BIOLOGIST). Returns 200 (idempotent), 409 when the
 * dossier is not fully bio-validated, 403 without the permission.
 */
@RestController
@RequestMapping("/api/v1/lab-files")
@RequiredArgsConstructor
public class SignController {

    private final SignDossierService signDossierService;

    @PostMapping("/{id}:sign")
    @RequiresPermission("dossier.sign-electronically")
    public ResponseEntity<Void> sign(
            @PathVariable String id, @Nullable Authentication authentication) {
        signDossierService.sign(
                new LabFileId(id), authentication == null ? null : authentication.getName());
        return ResponseEntity.ok().build(); // 200, idempotent
    }
}
