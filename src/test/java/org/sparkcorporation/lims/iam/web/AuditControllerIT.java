package org.sparkcorporation.lims.iam.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;

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

    @Test
    void admin_lists_signed_actions_for_a_subject() {
        var admin = restClientFor(port, "ADMIN");
        String adminId = userId("admin@test.local");

        // issue an authenticated request so the filter records at least one signed action
        admin.get().uri("/api/v1/me").retrieve().toBodilessEntity();
        await().atMost(Duration.ofSeconds(5))
                .until(() -> signedActionCount(adminId) >= 1);

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                admin.get()
                        .uri("/api/v1/audit/actions?subject=" + adminId)
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
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
        await().atMost(Duration.ofSeconds(5))
                .until(() -> signedActionCount(adminId) >= 1);

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
