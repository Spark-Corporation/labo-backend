package org.sparkcorporation.lims.iam.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import dev.sparksam.securedx.core.signing.ActionSigner;
import dev.sparksam.securedx.core.signing.UserKeyPairManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.iam.infra.AuditCheckpointService;
import org.sparkcorporation.lims.iam.infra.AuditCheckpointVerifier;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the checkpoint-backed verify endpoint ({@code GET
 * /api/v1/audit/checkpoints/latest}). Proves byte-identity between the verifier's recomputed
 * canonical payload and what {@link AuditCheckpointService} signed ({@code signatureValid=true}),
 * staleness detection ({@code headsMatch}, {@code actionsAppendedSince}), and the 404/403 contract.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditCheckpointVerifyIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserKeyPairManager keyPairManager;
    @Autowired private ActionSigner actionSigner;
    @Autowired private AuditCheckpointService checkpointService;
    @Autowired private AuditCheckpointVerifier checkpointVerifier;

    @Test
    void fresh_checkpoint_verifies_then_goes_stale_after_a_new_action() {
        String subject = seedSubjectWithKey();
        signAs(subject, "verify.seed", "/r/seed", Map.of("k", "v"));

        checkpointService.createCheckpoint();

        // Verify at the service level immediately after creation: no HTTP request intervenes, so no
        // new action is signed between createCheckpoint and the head recompute. signatureValid=true
        // proves the verifier rebuilds the exact bytes that were signed (byte-identity); the heads
        // still match and nothing has been appended.
        var fresh = checkpointVerifier.verifyLatest().orElseThrow();
        assertThat(fresh.signatureValid()).isTrue();
        assertThat(fresh.headsMatch()).isTrue();
        assertThat(fresh.actionsAppendedSince()).isZero();

        // Append a NEW action after the checkpoint — the latest checkpoint is now stale.
        signAs(subject, "verify.after", "/r/after", Map.of("k", "v"));

        var stale = checkpointVerifier.verifyLatest().orElseThrow();
        // Signature still valid (payload rebuilt from stored fields), but heads have moved on.
        assertThat(stale.signatureValid()).isTrue();
        assertThat(stale.headsMatch()).isFalse();
        assertThat(stale.actionsAppendedSince()).isPositive();
    }

    @Test
    void admin_gets_the_latest_checkpoint_verification_over_http() {
        String subject = seedSubjectWithKey();
        signAs(subject, "verify.http", "/r/http", Map.of("k", "v"));
        checkpointService.createCheckpoint();

        var admin = restClientFor(port, "ADMIN");

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                admin.get().uri("/api/v1/audit/checkpoints/latest").retrieve().body(Map.class);

        assertThat(body).isNotNull();
        assertThat(body)
                .containsKeys(
                        "checkpointId",
                        "globalSequence",
                        "merkleRoot",
                        "signatureValid",
                        "headsMatch",
                        "actionsAppendedSince");
        // The signature verifies regardless of subsequent ledger movement (it is rebuilt from the
        // stored checkpoint fields, not the live heads).
        assertThat(body.get("signatureValid")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void receptionist_without_audit_read_gets_403() {
        var receptionist = restClientFor(port, "RECEPTIONIST");

        HttpStatusCode status =
                receptionist
                        .get()
                        .uri("/api/v1/audit/checkpoints/latest")
                        .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // The "no checkpoint → 404" path is covered deterministically by
    // AuditCheckpointVerifyControllerTest (a @WebMvcTest with a mocked verifier returning empty):
    // audit_checkpoints is append-only (DB RULE blocks DELETE) and the IT suite shares one Postgres
    // container, so an empty-table state cannot be reliably reproduced from an IT once any other
    // test has created a checkpoint.

    private String seedSubjectWithKey() {
        String subject = Ulid.generate();
        jdbc.update(
                "INSERT INTO users (id, email, full_name, password_hash, enabled, version)"
                        + " VALUES (?, ?, 'Verify Test', '{argon2id}x', true, 0)",
                subject,
                "verify-" + subject + "@test.local");
        keyPairManager.generateKeyPair(subject);
        return subject;
    }

    private void signAs(String subject, String action, String resource, Map<String, Object> meta) {
        var auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        subject, null, List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(auth);
        try {
            actionSigner.sign(action, resource, meta);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
