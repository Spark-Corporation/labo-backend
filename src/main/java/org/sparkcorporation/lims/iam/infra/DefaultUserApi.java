package org.sparkcorporation.lims.iam.infra;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.iam.api.Role;
import org.sparkcorporation.lims.iam.api.UserApi;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.iam.api.UserSummary;
import org.sparkcorporation.lims.iam.domain.User;
import org.sparkcorporation.lims.iam.domain.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultUserApi implements UserApi {

    private final UserRepository repo;

    @Override
    public Optional<UserSummary> findById(UserId id) {
        return repo.findById(id.value()).map(DefaultUserApi::toSummary);
    }

    @Override
    public UserSummary requireById(UserId id) {
        return repo.findById(id.value())
                .map(DefaultUserApi::toSummary)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public boolean existsById(UserId id) {
        return repo.existsById(id.value());
    }

    @Override
    public Map<UserId, UserSummary> findAllByIds(Set<UserId> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        var rawIds = ids.stream().map(UserId::value).collect(Collectors.toUnmodifiableSet());
        return repo.findAllByIdIn(rawIds).stream()
                .map(DefaultUserApi::toSummary)
                .collect(Collectors.toUnmodifiableMap(UserSummary::id, s -> s));
    }

    private static UserSummary toSummary(User u) {
        var roles =
                u.roles().stream()
                        .map(r -> Role.valueOf(r.name()))
                        .collect(Collectors.toUnmodifiableSet());
        return new UserSummary(u.id(), u.email(), u.fullName(), u.enabled(), roles);
    }
}
