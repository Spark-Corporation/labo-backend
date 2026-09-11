package org.sparkcorporation.lims.billing.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * REST endpoints for {@code Payment} (spec §7.4):
 *
 * <ul>
 *   <li>{@code POST /api/v1/payments} — record a payment; permission {@code payment.collect}.
 *   <li>{@code GET /api/v1/lab-files/{labFileId}/payments} — list payments for a lab-file;
 *       permission {@code invoice.read}.
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @PostMapping("/api/v1/payments")
    @RequiresPermission("payment.collect")
    @Operation(
            summary = "Record a payment",
            description =
                    "Records a payment made against a lab file's invoice through a given "
                            + "cash register, and links it to the invoice if one has been issued.")
    public ResponseEntity<PaymentResponse> record(@Valid @RequestBody PaymentRequest req) {
        var resp = service.record(req, currentUserId());
        return ResponseEntity.created(URI.create("/api/v1/payments/" + resp.id())).body(resp);
    }

    @GetMapping("/api/v1/lab-files/{labFileId}/payments")
    @RequiresPermission("invoice.read")
    @Operation(
            summary = "List a lab file's payments",
            description = "Lists every payment recorded against a lab file, oldest first.")
    public List<PaymentResponse> listByLabFile(@PathVariable String labFileId) {
        return service.findByLabFileId(new LabFileId(labFileId));
    }

    /**
     * Mirrors {@code ResultAdminController.currentUserId()} — reads the principal name from the
     * Spring Security context. Throws if unauthenticated; {@code @RequiresPermission} should have
     * already rejected such requests at the filter level.
     */
    private static String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("no authenticated user in SecurityContext");
        }
        return auth.getName();
    }
}
