package org.sparkcorporation.lims.iam.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.sparksam.securedx.core.signing.ActionSignatureVerifier;
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
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the ADMIN-only audit read endpoints (CDC §II action history):
 *
 * <ul>
 *   <li>{@code GET /api/v1/audit/actions} — paged signed-action history, {@code audit.read}.
 *   <li>{@code POST /api/v1/audit/verify} — hash-chain verification, {@code audit.read}.
 * </ul>
 *
 * The per-request {@code ActionSigningFilter} signs every authenticated staff request, so an ADMIN
 * that has issued at least one authenticated request has a non-empty signed-action chain.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditControllerIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private ActionSignatureVerifier verifier;

    @Autowired private ActionSigner actionSigner;

    @Autowired private UserKeyPairManager keyPairManager;

    @Test
    void admin_lists_signed_actions_for_a_subject() {
        var admin = restClientFor(port, "ADMIN");
        String adminId = userId("admin@test.local");

        // issue an authenticated request so the filter records at least one signed action
        admin.get().uri("/api/v1/me").retrieve().toBodilessEntity();
        await().atMost(Duration.ofSeconds(5)).until(() -> signedActionCount(adminId) >= 1);

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                admin.get()
                        .uri("/api/v1/audit/actions?subject=" + adminId)
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        assertThat(body).containsKeys("content", "page", "size", "totalElements", "totalPages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        assertThat(content).isNotEmpty();
        var first = content.getFirst();
        assertThat(first).containsKeys("actionId", "action", "resource", "sequenceNumber");
        assertThat(first).doesNotContainKey("signature");
    }

    @Test
    void receptionist_without_audit_read_gets_403() {
        var receptionist = restClientFor(port, "RECEPTIONIST");
        String adminId = userId("admin@test.local");

        HttpStatusCode status =
                receptionist
                        .get()
                        .uri("/api/v1/audit/actions?subject=" + adminId)
                        .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_verifies_a_valid_chain() {
        var admin = restClientFor(port, "ADMIN");
        String adminId = userId("admin@test.local");

        admin.get().uri("/api/v1/me").retrieve().toBodilessEntity();
        await().atMost(Duration.ofSeconds(5)).until(() -> signedActionCount(adminId) >= 1);

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                admin.post()
                        .uri("/api/v1/audit/verify?subject=" + adminId)
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        assertThat(body.get("subject")).isEqualTo(adminId);
        assertThat(body.get("valid")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void update_and_delete_on_signed_actions_are_no_ops() {
        var admin = restClientFor(port, "ADMIN");
        String adminId = userId("admin@test.local");

        admin.get().uri("/api/v1/me").retrieve().toBodilessEntity();
        await().atMost(Duration.ofSeconds(5)).until(() -> signedActionCount(adminId) >= 1);

        long before = signedActionCount(adminId);
        String signatureBefore = latestSignature(adminId);

        // The append-only RULEs (no_update_signed_actions / no_delete_signed_actions) rewrite any
        // UPDATE/DELETE to DO INSTEAD NOTHING, so both affect zero rows and the ledger is
        // unchanged.
        int updated =
                jdbc.update(
                        "UPDATE signed_actions SET signature = 'tampered' WHERE subject = ?",
                        adminId);
        int deleted = jdbc.update("DELETE FROM signed_actions WHERE subject = ?", adminId);

        assertThat(updated).isZero();
        assertThat(deleted).isZero();
        assertThat(signedActionCount(adminId)).isEqualTo(before);
        assertThat(latestSignature(adminId)).isEqualTo(signatureBefore);
    }

    @Test
    void verify_reports_invalid_when_a_tampered_link_is_appended() {
        // Fresh, isolated subject: the IT suite shares one Postgres with no per-test reset, so we
        // must not pollute the shared admin chain that the other tests assert is valid.
        String subject = Ulid.generate();
        jdbc.update(
                "INSERT INTO users (id, email, full_name, password_hash, enabled, version)"
                        + " VALUES (?, ?, 'Tamper Test', '{argon2id}x', true, 0)",
                subject,
                "tamper-" + subject + "@test.local");
        keyPairManager.generateKeyPair(subject);

        // A genuine genesis link — the chain verifies clean at this point.
        signAs(subject, "test.action", "res-1", Map.of("k", "v"));
        assertThat(verifier.verifyChain(subject)).isTrue();

        // Append a structurally-present but cryptographically invalid second link: the genesis
        // signature reused at sequence 2 with a bogus previous_hash cannot match link 2's content,
        // so the signature/linkage checks fail. INSERT is allowed (only UPDATE/DELETE are
        // rule-blocked); reusing a real signature keeps it valid base64 so the verifier rejects it
        // on content, not on a decode error.
        String keyId = latestColumn(subject, "key_id");
        String genesisSignature = latestSignature(subject);
        jdbc.update(
                "INSERT INTO signed_actions (action_id, subject, action, resource, timestamp,"
                        + " key_id, previous_hash, sequence_number, signature)"
                        + " VALUES (?, ?, 'tampered.action', 'res-2', ?, ?, ?, 2, ?)",
                UUID.randomUUID().toString(),
                subject,
                Instant.now().toString(),
                keyId,
                "0".repeat(64),
                genesisSignature);

        assertThat(verifier.verifyChain(subject)).isFalse();
    }

    private String latestSignature(String subject) {
        return latestColumn(subject, "signature");
    }

    private String latestColumn(String subject, String column) {
        return jdbc.queryForObject(
                "SELECT "
                        + column
                        + " FROM signed_actions WHERE subject = ?"
                        + " ORDER BY sequence_number DESC LIMIT 1",
                String.class,
                subject);
    }

    private dev.sparksam.securedx.core.signing.SignedAction signAs(
            String subject, String action, String resource, Map<String, Object> meta) {
        var auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        subject, null, List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(auth);
        try {
            return actionSigner.sign(action, resource, meta);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    private String userId(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", String.class, email);
    }

    private long signedActionCount(String subject) {
        Long n =
                jdbc.queryForObject(
                        "SELECT count(*) FROM signed_actions WHERE subject = ?",
                        Long.class,
                        subject);
        return n == null ? 0L : n;
    }
}
