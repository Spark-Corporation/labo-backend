package org.sparkcorporation.lims.iam.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;

/**
 * Verifies the per-request {@code ActionSigningFilter} (provided by secured-x, enabled via {@code
 * secured-x.signing.filter.enabled=true} in {@code application.yml}) records a tamper-evident
 * {@code signed_actions} row for every authenticated staff request, and that the configured
 * exclude-path prefixes (e.g. {@code /api/v1/portal}) are NOT signed.
 *
 * <p>The filter matches {@code exclude-paths} by {@code String.startsWith} (prefix), not Ant glob.
 * It is fail-open: a signing error is logged and the request proceeds.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActionAuditFilterIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @Test
    void authenticated_staff_request_is_recorded_in_signed_actions() {
        var admin = restClientFor(port, "ADMIN"); // login provisions a key
        long before = count("SELECT count(*) FROM signed_actions");

        // GET /api/v1/me: a plain authenticated staff endpoint (no special permission, not an
        // excluded path). The plan's draft used /api/v1/lab-files, but that search endpoint
        // currently 500s on an all-null filter call (a pre-existing PostgreSQL null-parameter
        // type-inference bug, unrelated to the signing filter under test here).
        admin.get().uri("/api/v1/me").retrieve().toBodilessEntity();

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () ->
                                assertThat(count("SELECT count(*) FROM signed_actions"))
                                        .isGreaterThan(before));
    }

    @Test
    void portal_path_is_not_signed() {
        long before = count("SELECT count(*) FROM signed_actions");

        // an anonymous portal call (404/401 is fine) must not add a signed_actions row
        try {
            restClientAnonymous(port)
                    .post()
                    .uri("/api/v1/portal/login")
                    .body(Map.of("portalLogin", "x", "password", "y"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ignored) {
            // expected: anonymous portal login with bogus credentials is rejected
        }

        assertThat(count("SELECT count(*) FROM signed_actions")).isEqualTo(before);
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0L : n;
    }
}
