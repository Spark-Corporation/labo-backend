package org.sparkcorporation.lims.iam.web;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.iam.api.Role;

// Security-sensitive: never log or echo the password value.
@Schema(description = "Request to create a new staff user account.")
public record CreateUserRequest(
        @Schema(description = "Login email; must be unique across all staff users.")
                @Email
                @NotBlank
                @Size(max = 160)
                String email,
        @Schema(description = "Display name of the user.") @NotBlank @Size(max = 160)
                String fullName,
        @Schema(description = "Initial password for the account; must be 8-100 characters.")
                @NotBlank
                @Size(min = 8, max = 100)
                String password,
        @Schema(description = "Roles to grant the user; at least one is required.") @NotEmpty
                Set<Role> roles) {}
