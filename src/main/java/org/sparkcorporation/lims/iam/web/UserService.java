package org.sparkcorporation.lims.iam.web;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.iam.api.Role;
import org.sparkcorporation.lims.iam.api.RoleAssigned;
import org.sparkcorporation.lims.iam.api.UserCreated;
import org.sparkcorporation.lims.iam.api.UserDisabled;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.iam.api.UserPasswordChanged;
import org.sparkcorporation.lims.iam.domain.EmailAlreadyExistsException;
import org.sparkcorporation.lims.iam.domain.User;
import org.sparkcorporation.lims.iam.domain.UserNotFoundException;
import org.sparkcorporation.lims.iam.infra.RoleRepository;
import org.sparkcorporation.lims.iam.infra.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;
    private final RoleRepository roleRepo;
    private final UserMapper mapper;
    private final ApplicationEventPublisher events;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (repo.existsByEmailIgnoreCase(req.email())) {
            throw new EmailAlreadyExistsException(req.email());
        }
        var roleNames = req.roles().stream().map(Enum::name).collect(Collectors.toSet());
        var roleEntities = new HashSet<>(roleRepo.findByNameIn(roleNames));
        var user =
                User.create(
                        req.email(),
                        req.fullName(),
                        passwordEncoder.encode(req.password()),
                        roleEntities);
        repo.save(user);
        events.publishEvent(new UserCreated(user.id(), user.email(), clock.instant()));
        return mapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UserId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String q, String roleName, int page, int size) {
        Page<User> result =
                repo.search(emptyToNull(q), emptyToNull(roleName), PageRequest.of(page, size));
        return result.map(mapper::toResponse);
    }

    @Transactional
    public UserResponse update(UserId id, UpdateUserRequest req) {
        var u = repo.findById(id.value()).orElseThrow(() -> new UserNotFoundException(id));
        var wasEnabled = u.enabled();
        u.update(mapper.toUpdateCommand(req));
        if (wasEnabled && !u.enabled()) {
            events.publishEvent(new UserDisabled(u.id(), clock.instant()));
        }
        return mapper.toResponse(u);
    }

    @Transactional
    public void delete(UserId id) {
        var u = repo.findById(id.value()).orElseThrow(() -> new UserNotFoundException(id));
        repo.delete(u);
    }

    @Transactional
    public UserResponse assignRoles(UserId id, Set<Role> roles) {
        var u = repo.findById(id.value()).orElseThrow(() -> new UserNotFoundException(id));
        var roleNames = roles.stream().map(Enum::name).collect(Collectors.toSet());
        var roleEntities = new HashSet<>(roleRepo.findByNameIn(roleNames));
        u.replaceRoles(roleEntities);
        for (Role role : roles) {
            events.publishEvent(new RoleAssigned(u.id(), role, clock.instant()));
        }
        return mapper.toResponse(u);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrent() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("no authentication context");
        }
        var id = new UserId(auth.getName());
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public void changePassword(UserId id, ChangePasswordRequest req) {
        var u = repo.findById(id.value()).orElseThrow(() -> new UserNotFoundException(id));
        if (!passwordEncoder.matches(req.oldPassword(), u.passwordHash())) {
            throw new IllegalArgumentException("old password does not match");
        }
        u.changePassword(passwordEncoder.encode(req.newPassword()));
        events.publishEvent(new UserPasswordChanged(u.id(), clock.instant()));
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
