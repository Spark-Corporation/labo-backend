package org.sparkcorporation.lims.iam.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.iam.domain.User;
import org.sparkcorporation.lims.iam.infra.UserRepository;

/**
 * Acceptance IT verifying that disabling a user blocks subsequent logins. The cashier test user is
 * toggled off, login is attempted (must fail with 401), and the user is restored to {@code enabled
 * = true} so other tests are unaffected.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DisabledUserAcceptanceIT extends IntegrationTestBase {

    @LocalServerPort private int port;
    @Autowired private UserRepository userRepo;

    @Test
    void disabling_user_blocks_subsequent_login() {
        User cashier = userRepo.findByEmailIgnoreCase("cashier@test.local").orElseThrow();
        cashier.disable();
        userRepo.saveAndFlush(cashier);

        RestClient http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        HttpStatusCode status =
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
                                                "cashier@test.local",
                                                "password",
                                                "test-password")))
                        .exchange((req, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Restore for other tests.
        cashier.enable();
        userRepo.saveAndFlush(cashier);
    }
}
