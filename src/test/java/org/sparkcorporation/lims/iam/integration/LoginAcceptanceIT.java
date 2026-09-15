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
 * /api/v1/auth/login} and receive a JWT access token. The seed lives in the {@code test} Liquibase
 * context (see {@code db/changelog/data/202605061002-seed-iam-test-users.yaml}).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginAcceptanceIT extends IntegrationTestBase {

    private static final Map<String, Object> ADMIN_CREDENTIALS =
            Map.of(
                    "provider",
                    "local",
                    "payload",
                    Map.of("username", "admin@test.local", "password", "test-password"));

    @LocalServerPort private int port;

    @Test
    @SuppressWarnings("rawtypes")
    void successful_login_returns_jwt_for_seeded_admin() {
        RestClient http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        ResponseEntity<Map> response =
                http.post()
                        .uri("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(ADMIN_CREDENTIALS)
                        .retrieve()
                        .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat((String) response.getBody().get("accessToken")).isNotBlank();
    }

    /**
     * secured-x hard-codes {@code /auth} on its controller; {@code AuthPathPrefixConfig} moves it
     * under {@code /api/v1}. Guards against the prefix being dropped while clients still log in at
     * the old path. With valid credentials, the unprefixed route must not issue a token: it is no
     * longer a public path, so the security filter rejects it with 401 before routing.
     */
    @Test
    void unprefixed_legacy_login_path_is_no_longer_served() {
        RestClient http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        var status =
                http.post()
                        .uri("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(ADMIN_CREDENTIALS)
                        .retrieve()
                        .onStatus(code -> true, (request, response) -> {})
                        .toBodilessEntity()
                        .getStatusCode();

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
