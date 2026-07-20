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

import org.sparkcorporation.lims.delivery.infra.SendDossierService;
import org.sparkcorporation.lims.delivery.infra.SendDossierService.SendDossierResult;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Staff result-delivery endpoint — {@code POST /api/v1/lab-files/{id}:send}. Guarded by {@code
 * dossier.send} (granted to ADMIN and BIOLOGIST). Encrypts the compte-rendu PDF, emails it, and
 * delivers the password out-of-band (WhatsApp, else returned to staff). Returns 202 with the
 * notification ids, 409 when the dossier is not PUBLISHED, 422 when the patient has no email, 403
 * without the permission.
 *
 * <p><strong>The response body may carry a secret {@code password}.</strong> This method never logs
 * the response; per the {@code /lab-files/*} redaction rule (spec §7), the password must not leak
 * into any log body.
 */
@RestController
@RequestMapping("/api/v1/lab-files")
@RequiredArgsConstructor
public class SendController {

    private final SendDossierService sendDossierService;

    @PostMapping("/{id}:send")
    @RequiresPermission("dossier.send")
    public ResponseEntity<SendDossierResponse> send(
            @PathVariable String id, @Nullable Authentication authentication) {
        SendDossierResult result =
                sendDossierService.send(
                        new LabFileId(id),
                        authentication == null ? null : authentication.getName());
        return ResponseEntity.accepted()
                .body(
                        new SendDossierResponse(
                                result.emailId(), result.whatsAppId(), result.password()));
    }
}
