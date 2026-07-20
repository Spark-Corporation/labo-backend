package org.sparkcorporation.lims.iam.domain;

import java.util.Set;

import org.sparkcorporation.lims.iam.api.Role;

/** Command for creating a {@link User}. {@code passwordHash} is pre-hashed (Argon2id). */
public record NewUser(String email, String fullName, String passwordHash, Set<Role> roles) {}
