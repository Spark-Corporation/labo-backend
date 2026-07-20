package org.sparkcorporation.lims.iam.web;

import java.time.Instant;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.iam.api.Role;

public record UserResponse(
        String id,
        String email,
        String fullName,
        boolean enabled,
        Set<Role> roles,
        @Nullable Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        int version) {}
