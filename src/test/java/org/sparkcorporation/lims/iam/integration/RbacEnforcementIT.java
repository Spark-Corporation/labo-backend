package org.sparkcorporation.lims.iam.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;

/**
 * Acceptance IT for the LIMS IAM 1.1.b RBAC enforcement: verifies the {@code @RequiresPermission}
 * advice on patient endpoints rejects unauthenticated and under-privileged callers, and admits
 * callers whose role grants the required permission.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RbacEnforcementIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Test
    void unauthenticated_request_returns_401() {
        RestClient http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        HttpStatusCode status =
                http.get()
                        .uri("/api/v1/patients/01HZX0000000000000000ABCD")
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void technician_calling_patient_merge_returns_403() {
        String techJwt = loginAs(port, "technician@test.local", "test-password");
        RestClient http =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        .defaultHeader("Authorization", "Bearer " + techJwt)
                        .build();

        HttpStatusCode status =
                http.post()
                        .uri("/api/v1/patients/01HZX0000000000000000ABCD/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("targetId", "01HZX0000000000000000ZZZZ"))
                        .exchange((req, res) -> res.getStatusCode());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void receptionist_can_call_patient_search() {
        String receptJwt = loginAs(port, "receptionist@test.local", "test-password");
        RestClient http =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        .defaultHeader("Authorization", "Bearer " + receptJwt)
                        .build();

        HttpStatusCode status =
                http.get()
                        .uri("/api/v1/patients?page=0&size=10")
                        .exchange((req, res) -> res.getStatusCode());
        // 200 OK (empty list is fine) — RBAC allows it
        assertThat(status).isEqualTo(HttpStatus.OK);
    }
}
