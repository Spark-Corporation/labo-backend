package org.sparkcorporation.lims.iam.infra;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.iam.domain.RoleEntity;
import org.sparkcorporation.lims.iam.domain.User;

/**
 * Bootstraps the first ADMIN user from environment variables on initial startup.
 *
 * <p>No-op when the {@code users} table is non-empty. When empty, requires {@code
 * LIMS_BOOTSTRAP_ADMIN_EMAIL} and {@code LIMS_BOOTSTRAP_ADMIN_PASSWORD} (mapped to {@code
 * lims.iam.bootstrap.admin-email/password}) to be set; fails fast otherwise.
 */
@Component
@ConditionalOnProperty(
        name = "lims.iam.bootstrap.enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class IamBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final Environment env;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepo.count() > 0) {
            log.debug("bootstrap skipped — users table not empty");
            return;
        }
        String email = env.getProperty("lims.iam.bootstrap.admin-email");
        String password = env.getProperty("lims.iam.bootstrap.admin-password");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "users table empty but LIMS_BOOTSTRAP_ADMIN_EMAIL/PASSWORD not set —"
                            + " cannot bootstrap");
        }
        RoleEntity adminRole =
                roleRepo.findByName("ADMIN")
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "ADMIN role not seeded — Liquibase seed missing"));

        User admin =
                User.create(email, "Bootstrap Admin", encoder.encode(password), Set.of(adminRole));
        userRepo.save(admin);
        log.warn("bootstrap admin created email={} ulid={}", email, admin.id().value());
    }
}
