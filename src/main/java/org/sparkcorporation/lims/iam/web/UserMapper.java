package org.sparkcorporation.lims.iam.web;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.iam.api.Role;
import org.sparkcorporation.lims.iam.domain.UpdateUser;
import org.sparkcorporation.lims.iam.domain.User;

/** Hand-written mapper between web DTOs and domain types. */
@Component
public class UserMapper {

    public UpdateUser toUpdateCommand(UpdateUserRequest req) {
        return new UpdateUser(req.email(), req.fullName(), req.enabled());
    }

    public UserResponse toResponse(User u) {
        var roles =
                u.roles().stream()
                        .map(r -> Role.valueOf(r.name()))
                        .collect(Collectors.toUnmodifiableSet());
        return new UserResponse(
                u.id().value(),
                u.email(),
                u.fullName(),
                u.enabled(),
                roles,
                u.lastLoginAt(),
                u.createdAt(),
                u.updatedAt(),
                u.version());
    }
}
