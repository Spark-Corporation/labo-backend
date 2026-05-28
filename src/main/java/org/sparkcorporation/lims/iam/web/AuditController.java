package org.sparkcorporation.lims.iam.web;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.core.signing.ActionSignatureVerifier;
import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.iam.infra.SignedActionRepository;

/**
 * ADMIN-only read side over the tamper-evident action-history ledger (CDC §II). Both endpoints are
 * guarded by {@code audit.read}, held by ADMIN only. The hash-chain itself is written append-only
 * by the per-request {@code ActionSigningFilter} and the explicit dossier-sign use-case; this
 * controller never mutates it.
 *
 * <p>⚠ security surface — read access to the signing/audit ledger ({@code iam/}). Second approval
 * required per CONTRIBUTING.md § PR lifecycle.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final SignedActionRepository repo;
    private final ActionSignatureVerifier verifier;

    /**
     * Paged signed-action history, optionally filtered by {@code subject} (the acting user's ULID)
     * or by {@code resource} (the signed request URI). When both are supplied {@code subject} wins;
     * when neither is supplied the full ledger is returned (most recent first).
     */
    @GetMapping("/actions")
    @RequiresPermission("audit.read")
    public Page<AuditActionResponse> actions(
            @RequestParam(required = false) @Nullable String subject,
            @RequestParam(required = false) @Nullable String resource,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size);
        Page<org.sparkcorporation.lims.iam.infra.SignedActionEntity> result;
        if (subject != null && !subject.isBlank()) {
            result = repo.findBySubject(subject, pageable);
        } else if (resource != null && !resource.isBlank()) {
            result = repo.findByResource(resource, pageable);
        } else {
            result = repo.findAll(pageable);
        }
        return result.map(AuditActionResponse::from);
    }

    /**
     * Verifies the integrity of {@code subject}'s entire hash-chain (each link's signature plus the
     * {@code previousHash} linkage). Returns {@code valid=false} for a tampered or broken chain;
     * a subject with no signed actions verifies as {@code true} (an empty chain is trivially
     * consistent).
     */
    @PostMapping("/verify")
    @RequiresPermission("audit.read")
    public Map<String, Object> verify(@RequestParam String subject) {
        boolean valid = verifier.verifyChain(subject);
        return Map.of("subject", subject, "valid", valid);
    }
}
