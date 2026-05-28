package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;

/**
 * Verifies that logging in provisions a per-user Ed25519 signing key pair on first authentication
 * (CDC §IV signing prerequisite). The seeded biologist (see {@code
 * db/changelog/data/202605061002-seed-iam-test-users.yaml}) has no key pair until they log in.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KeyProvisioningOnLoginIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    @Test
    void login_provisions_an_active_key_pair_for_the_user() {
        String userId =
                jdbc.queryForObject(
                        "SELECT id FROM users WHERE email = ?",
                        String.class,
                        "biologist@test.local");
        assertThat(activeKeyCount(userId)).isZero();

        loginAs(port, "biologist@test.local", "test-password");

        assertThat(activeKeyCount(userId)).isEqualTo(1);
    }

    private int activeKeyCount(String subject) {
        Integer count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM user_key_pairs WHERE subject = ? AND active = true",
                        Integer.class,
                        subject);
        return count == null ? 0 : count;
    }
}
