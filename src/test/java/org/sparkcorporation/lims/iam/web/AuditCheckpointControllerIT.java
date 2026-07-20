package org.sparkcorporation.lims.iam.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import org.sparkcorporation.lims.IntegrationTestBase;

/**
 * Acceptance IT for the ADMIN-only checkpoint-creation endpoint (CDC §II): {@code POST
 * /api/v1/audit/checkpoints} — guarded by {@code audit.checkpoint}, held by ADMIN only.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditCheckpointControllerIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Test
    void admin_creates_a_checkpoint() {
        var admin = restClientFor(port, "ADMIN");

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                admin.post()
                        .uri("/api/v1/audit/checkpoints")
                        .exchange(
                                (req, res) -> {
                                    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                                    return res.bodyTo(Map.class);
                                });

        assertThat(body).isNotNull();
        assertThat(body)
                .containsKeys("checkpointId", "globalSequence", "merkleRoot", "subjectCount");
    }

    @Test
    void receptionist_without_audit_checkpoint_gets_403() {
        var receptionist = restClientFor(port, "RECEPTIONIST");

        HttpStatusCode status =
                receptionist
                        .post()
                        .uri("/api/v1/audit/checkpoints")
                        .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
