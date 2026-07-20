package org.sparkcorporation.lims.iam.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;

/**
 * Acceptance IT for the LIMS IAM 1.1.b PR DoD: a seeded admin user can log in via {@code
 * /auth/login} and receive a JWT access token. The seed lives in the {@code test} Liquibase context
 * (see {@code db/changelog/data/202605061002-seed-iam-test-users.yaml}).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginAcceptanceIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Test
    @SuppressWarnings("rawtypes")
    void successful_login_returns_jwt_for_seeded_admin() {
        RestClient http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        ResponseEntity<Map> response =
                http.post()
                        .uri("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "provider",
                                        "local",
                                        "payload",
                                        Map.of(
                                                "username",
                                                "admin@test.local",
                                                "password",
                                                "test-password")))
                        .retrieve()
                        .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat((String) response.getBody().get("accessToken")).isNotBlank();
    }
}
