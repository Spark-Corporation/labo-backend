package org.sparkcorporation.lims.iam.web;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.iam.infra.AuditCheckpointService;

/**
 * ADMIN-only checkpoint-creation surface over the tamper-evident action-history ledger (CDC §II).
 * Guarded by {@code audit.checkpoint}, held by ADMIN only. Each call snapshots every subject's
 * chain head, computes a Merkle root, signs the canonical payload with the system audit identity,
 * and persists the checkpoint; the underlying hash-chain is never mutated.
 *
 * <p>⚠ security surface — write access to the signing/audit ledger ({@code iam/}). Second approval
 * required per CONTRIBUTING.md § PR lifecycle.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditCheckpointController {

    private final AuditCheckpointService service;

    @PostMapping("/checkpoints")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("audit.checkpoint")
    @Operation(
            summary = "Create an audit checkpoint",
            description =
                    "Snapshots every subject's current hash-chain head, computes a Merkle root over"
                        + " them, signs the canonical payload with the system audit identity, and"
                        + " persists the checkpoint. The underlying hash-chain itself is never"
                        + " mutated.")
    public AuditCheckpointResponse create() {
        return AuditCheckpointResponse.from(service.createCheckpoint());
    }
}
