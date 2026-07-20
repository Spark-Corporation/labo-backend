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
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the per-dossier action history endpoint ({@code GET
 * /api/v1/audit/lab-files/{labFileId}/actions}). The dossier scope is derived from the signed
 * request {@code resource} URI: an action is "in" the dossier iff its {@code resource} contains the
 * lab-file ULID. This documents (and tests) the URI-derived limitation — actions whose resource
 * does not embed the lab-file ULID (e.g. {@code /api/v1/results/{rid}}) are NOT returned.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditDossierHistoryIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserKeyPairManager keyPairManager;
    @Autowired private ActionSigner actionSigner;

    @Test
    void admin_lists_only_dossier_scoped_actions() {
        String subject = seedSubjectWithKey();
        String labFileId = Ulid.generate();

        signAs(subject, "labfile.read", "/api/v1/lab-files/" + labFileId + "/analyses", Map.of());
        signAs(subject, "labfile.update", "/api/v1/lab-files/" + labFileId, Map.of());
        // unrelated quote action — different resource, must NOT appear
        signAs(subject, "quote.read", "/api/v1/quotes/" + Ulid.generate(), Map.of());
        // results action — URI does not embed the lab-file ULID, documents the limitation
        signAs(subject, "result.read", "/api/v1/results/" + Ulid.generate(), Map.of());

        var admin = restClientFor(port, "ADMIN");

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                admin.get()
                        .uri("/api/v1/audit/lab-files/" + labFileId + "/actions")
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        // Every returned row's resource embeds the labFileId (the URI-derived scope). The ADMIN's
        // own audit GET is itself signed against this same /lab-files/{labFileId}/actions URI, so
        // it
        // legitimately appears too — we assert on the seeded dossier actions and on absence of the
        // out-of-scope ones rather than an exact count.
        assertThat(content)
                .allSatisfy(row -> assertThat((String) row.get("resource")).contains(labFileId));
        assertThat(content)
                .extracting(row -> (String) row.get("action"))
                .contains("labfile.read", "labfile.update")
                .doesNotContain("quote.read", "result.read");
    }

    @Test
    void receptionist_without_audit_read_gets_403() {
        var receptionist = restClientFor(port, "RECEPTIONIST");

        HttpStatusCode status =
                receptionist
                        .get()
                        .uri("/api/v1/audit/lab-files/" + Ulid.generate() + "/actions")
                        .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String seedSubjectWithKey() {
        String subject = Ulid.generate();
        jdbc.update(
                "INSERT INTO users (id, email, full_name, password_hash, enabled, version)"
                        + " VALUES (?, ?, 'Dossier Test', '{argon2id}x', true, 0)",
                subject,
                "dossier-" + subject + "@test.local");
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
