package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.sparkcorporation.lims.iam.domain.RoleEntity;

class IamBootstrapRunnerTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final RoleRepository roleRepo = mock(RoleRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final Environment env = mock(Environment.class);
    private final IamBootstrapRunner runner =
            new IamBootstrapRunner(userRepo, roleRepo, encoder, env);

    private final ApplicationArguments args = mock(ApplicationArguments.class);

    @Test
    void noop_when_users_table_not_empty() {
        when(userRepo.count()).thenReturn(5L);
        runner.run(args);
        verify(userRepo, never()).save(any());
    }

    @Test
    void fails_when_users_empty_and_env_missing() {
        when(userRepo.count()).thenReturn(0L);
        when(env.getProperty("lims.iam.bootstrap.admin-email")).thenReturn(null);
        assertThatThrownBy(() -> runner.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LIMS_BOOTSTRAP_ADMIN");
    }

    @Test
    void creates_admin_user_when_users_empty_and_env_provided() {
        when(userRepo.count()).thenReturn(0L);
        when(env.getProperty("lims.iam.bootstrap.admin-email")).thenReturn("admin@local.dev");
        when(env.getProperty("lims.iam.bootstrap.admin-password")).thenReturn("dev-pwd");
        when(encoder.encode("dev-pwd")).thenReturn("{argon2id}fakehash");
        var adminRole = mock(RoleEntity.class);
        when(adminRole.name()).thenReturn("ADMIN");
        when(roleRepo.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        runner.run(args);

        verify(userRepo).save(any());
    }
}
