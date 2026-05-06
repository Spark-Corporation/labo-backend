package org.sparkcorporation.lims;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@Testcontainers
public abstract class IntegrationTestBase {

    @Container @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("lims")
                    .withUsername("lims")
                    .withPassword("lims");

    /**
     * Logs in via secured-x's {@code /auth/login} endpoint with provider {@code local} and returns
     * the issued JWT access token. The caller is responsible for attaching it as a {@code
     * Authorization: Bearer <jwt>} header on subsequent requests.
     *
     * <p>The seeded test admin (see Chunk 1 Task 1.5) is {@code admin@test.local} / {@code
     * test-password}.
     */
    protected static String loginAs(int port, String email, String password) {
        var http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        var body =
                Map.of(
                        "provider",
                        "local",
                        "payload",
                        Map.of("username", email, "password", password));
        @SuppressWarnings("unchecked")
        Map<String, Object> response =
                http.post()
                        .uri("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);
        return (String) response.get("accessToken");
    }
}
