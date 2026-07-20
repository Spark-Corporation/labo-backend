package org.sparkcorporation.lims.iam.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.sparkcorporation.lims.iam.api.Role;
import org.sparkcorporation.lims.iam.api.UserCreated;
import org.sparkcorporation.lims.iam.api.UserDisabled;
import org.sparkcorporation.lims.iam.api.UserPasswordChanged;
import org.sparkcorporation.lims.iam.domain.EmailAlreadyExistsException;
import org.sparkcorporation.lims.iam.domain.RoleEntity;
import org.sparkcorporation.lims.iam.domain.User;
import org.sparkcorporation.lims.iam.infra.RoleRepository;
import org.sparkcorporation.lims.iam.infra.UserRepository;

class UserServiceTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final RoleRepository roleRepo = mock(RoleRepository.class);
    private final UserMapper mapper = new UserMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
    private final UserService service =
            new UserService(repo, roleRepo, mapper, events, encoder, clock);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static CreateUserRequest createReq() {
        return new CreateUserRequest(
                "alice@lab.tg", "Alice", "plain-password", Set.of(Role.TECHNICIAN));
    }

    private static RoleEntity mockRoleEntity(String name) {
        var r = mock(RoleEntity.class);
        when(r.name()).thenReturn(name);
        return r;
    }

    @Test
    void create_hashes_password_and_publishes_event() {
        var techRole = mockRoleEntity("TECHNICIAN");
        when(repo.existsByEmailIgnoreCase("alice@lab.tg")).thenReturn(false);
        when(roleRepo.findByNameIn(any())).thenReturn(List.of(techRole));
        when(encoder.encode("plain-password")).thenReturn("{argon2id}fakehash");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.create(createReq());

        assertThat(resp.email()).isEqualTo("alice@lab.tg");
        verify(encoder).encode("plain-password");
        verify(events).publishEvent(any(UserCreated.class));
    }

    @Test
    void create_throws_when_email_exists() {
        when(repo.existsByEmailIgnoreCase("alice@lab.tg")).thenReturn(true);
        assertThatThrownBy(() -> service.create(createReq()))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void update_publishes_UserDisabled_when_enabled_flips_to_false() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));

        service.update(u.id(), new UpdateUserRequest(null, null, false));

        verify(events).publishEvent(any(UserDisabled.class));
    }

    @Test
    void delete_soft_deletes_via_repo() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));

        service.delete(u.id());

        verify(repo).delete(u);
    }

    @Test
    void assignRoles_replaces_role_set() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        var techRole = mockRoleEntity("TECHNICIAN");
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        when(roleRepo.findByNameIn(any())).thenReturn(List.of(techRole));

        service.assignRoles(u.id(), Set.of(Role.TECHNICIAN));

        // Replaced via User.replaceRoles — verified indirectly: u.hasRoleNamed("TECHNICIAN") true
        assertThat(u.hasRoleNamed("TECHNICIAN")).isTrue();
    }

    @Test
    void getCurrent_returns_caller_from_SecurityContext() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        var auth = new UsernamePasswordAuthenticationToken(u.id().value(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));

        var resp = service.getCurrent();

        assertThat(resp.email()).isEqualTo("a@b.c");
    }

    @Test
    void changePassword_throws_when_oldPassword_mismatches() {
        var u = User.create("a@b.c", "Alice", "{argon2id}old", Set.of());
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        when(encoder.matches("wrong", "{argon2id}old")).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.changePassword(
                                        u.id(),
                                        new ChangePasswordRequest("wrong", "newPassword123")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePassword_persists_new_hash_and_publishes_event() {
        var u = User.create("a@b.c", "Alice", "{argon2id}old", Set.of());
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        when(encoder.matches("oldPlain", "{argon2id}old")).thenReturn(true);
        when(encoder.encode("newPassword123")).thenReturn("{argon2id}new");

        service.changePassword(u.id(), new ChangePasswordRequest("oldPlain", "newPassword123"));

        assertThat(u.passwordHash()).isEqualTo("{argon2id}new");
        verify(events).publishEvent(any(UserPasswordChanged.class));
    }
}
