package org.sparkcorporation.lims.iam.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

public record UpdateUserRequest(
        @Email @Size(max = 160) @Nullable String email,
        @Size(max = 160) @Nullable String fullName,
        @Nullable Boolean enabled) {}
