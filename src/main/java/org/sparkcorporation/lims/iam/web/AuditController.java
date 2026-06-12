package org.sparkcorporation.lims.iam.web;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.core.signing.ActionSignatureVerifier;
import dev.sparksam.securedx.rbac.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.iam.infra.AuditCheckpointVerifier;
import org.sparkcorporation.lims.iam.infra.SignedActionEntity;
import org.sparkcorporation.lims.iam.infra.SignedActionRepository;
import org.sparkcorporation.lims.iam.infra.SignedActionSpecifications;

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
    private final AuditCheckpointVerifier checkpointVerifier;

    /**
     * Paged signed-action history. The legacy filters {@code subject} (the acting user's ULID) and
     * {@code resource} (the signed request URI) behave as before: when both are supplied {@code
     * subject} wins, and when neither is supplied the full ledger is returned.
     *
     * <p>The additive filters {@code action} (exact match), {@code from} and {@code to} (inclusive
     * ISO-8601 instant bounds on the action timestamp) compose with {@code subject} / {@code
     * resource}. Supplying any of {@code action} / {@code from} / {@code to} routes the query
     * through a {@link SignedActionSpecifications specification} that AND-composes every supplied
     * criterion; otherwise the legacy derived-query path is used unchanged. A {@code from} / {@code
     * to} that is not a valid ISO-8601 instant yields {@code 400} via the shared RFC 7807 mapper.
     */
    @GetMapping("/actions")
    @RequiresPermission("audit.read")
    public AuditActionSearchResponse actions(
            @RequestParam(required = false) @Nullable String subject,
            @RequestParam(required = false) @Nullable String resource,
            @RequestParam(required = false) @Nullable String action,
            @RequestParam(required = false) @Nullable String from,
            @RequestParam(required = false) @Nullable String to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size);
        Page<SignedActionEntity> result;
        if (hasText(action) || hasText(from) || hasText(to)) {
            String fromIso = normalizeInstant(from, "from");
            String toIso = normalizeInstant(to, "to");
            result =
                    repo.findAll(
                            SignedActionSpecifications.build(
                                    subject, resource, action, fromIso, toIso),
                            pageable);
        } else if (hasText(subject)) {
            result = repo.findBySubject(subject, pageable);
        } else if (hasText(resource)) {
            result = repo.findByResource(resource, pageable);
        } else {
            result = repo.findAll(pageable);
        }
        Page<AuditActionResponse> mapped = result.map(AuditActionResponse::from);
        return new AuditActionSearchResponse(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages());
    }

    private static boolean hasText(@Nullable String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Validates an optional instant bound and returns its canonical ISO-8601 string (or {@code
     * null} when absent), so the lexical range in {@link SignedActionSpecifications} matches the
     * stored {@code timestamp} TEXT. A malformed value raises {@link IllegalArgumentException},
     * which the shared advice maps to {@code 400}.
     */
    private static @Nullable String normalizeInstant(@Nullable String value, String paramName) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value).toString();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "invalid ISO-8601 instant for parameter '" + paramName + "'", ex);
        }
    }

    /**
     * Per-dossier action history: the signed actions whose {@code resource} URI embeds the given
     * lab-file ULID, newest first.
     *
     * <p>The dossier scope is <em>URI-derived</em>: an action belongs to the dossier iff its signed
     * {@code resource} string contains {@code labFileId} (Spring derives {@code LIKE %labFileId%}).
     * This captures every {@code /api/v1/lab-files/{labFileId}/...} request but deliberately misses
     * actions whose URI does not embed the lab-file ULID even though they concern the same dossier
     * (e.g. {@code /api/v1/results/{resultId}} where the dossier is reachable only via a join).
     * Such cross-referenced actions are out of scope for this endpoint.
     */
    @GetMapping("/lab-files/{labFileId}/actions")
    @RequiresPermission("audit.read")
    public AuditActionSearchResponse dossierActions(
            @PathVariable String labFileId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditActionResponse> mapped =
                repo.findByResourceContaining(labFileId, pageable).map(AuditActionResponse::from);
        return new AuditActionSearchResponse(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages());
    }

    /**
     * Verifies the integrity of {@code subject}'s entire hash-chain (each link's signature plus the
     * {@code previousHash} linkage). Returns {@code valid=false} for a tampered or broken chain; a
     * subject with no signed actions verifies as {@code true} (an empty chain is trivially
     * consistent).
     */
    @PostMapping("/verify")
    @RequiresPermission("audit.read")
    public Map<String, Object> verify(@RequestParam String subject) {
        boolean valid = verifier.verifyChain(subject);
        return Map.of("subject", subject, "valid", valid);
    }

    /**
     * Re-verifies the most recent audit checkpoint against the live ledger: whether its signature
     * still verifies (payload rebuilt from the stored fields), whether its Merkle root still
     * matches the current chain heads ({@code headsMatch}), and how many signed actions have been
     * appended since ({@code actionsAppendedSince}). Returns {@code 404} when no checkpoint has
     * been created.
     */
    @GetMapping("/checkpoints/latest")
    @RequiresPermission("audit.read")
    public CheckpointVerifyResponse latestCheckpoint() {
        return checkpointVerifier
                .verifyLatest()
                .map(CheckpointVerifyResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("no audit checkpoint exists"));
    }
}
