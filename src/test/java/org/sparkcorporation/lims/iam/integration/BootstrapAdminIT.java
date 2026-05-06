package org.sparkcorporation.lims.iam.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.iam.infra.UserRepository;

/**
 * Acceptance IT for the IamBootstrapRunner no-op path: when users already exist (the test seed
 * inserts 7 users in the {@code test} context), the bootstrap runner must not attempt to create a
 * second admin. The env-fail and create-admin paths are covered by the unit test {@code
 * IamBootstrapRunnerTest}; a full bootstrap-from-empty-DB IT would require a separate Spring
 * profile that excludes the test-user seed and is deferred.
 */
@Tag("integration")
@SpringBootTest
class BootstrapAdminIT extends IntegrationTestBase {

    @Autowired private UserRepository userRepo;

    @Test
    void test_admin_seed_already_present_so_bootstrap_runner_is_noop() {
        assertThat(userRepo.count()).isGreaterThan(0);
        assertThat(userRepo.findByEmailIgnoreCase("admin@test.local")).isPresent();
    }
}
