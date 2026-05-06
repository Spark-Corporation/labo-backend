package org.sparkcorporation.lims.iam.web;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import org.sparkcorporation.lims.iam.api.Role;

public record CreateUserRequest(
        @Email @NotBlank @Size(max = 160) String email,
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotEmpty Set<Role> roles) {}
