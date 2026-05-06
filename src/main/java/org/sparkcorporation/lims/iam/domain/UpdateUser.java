package org.sparkcorporation.lims.iam.domain;

import org.jspecify.annotations.Nullable;

/**
 * Partial-update command. Null fields are ignored. Password is NOT updated here — see PATCH
 * /me/password.
 */
public record UpdateUser(
        @Nullable String email, @Nullable String fullName, @Nullable Boolean enabled) {}
