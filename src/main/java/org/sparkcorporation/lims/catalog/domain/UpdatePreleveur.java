package org.sparkcorporation.lims.catalog.domain;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.Habilitation;

/**
 * All fields nullable; null = no change. Code is NOT modifiable (server-managed). PATCH semantics
 * for {@code habilitations}: {@code null} = no change; non-null empty {@code Set} = invariant
 * violation; non-null non-empty = full replace.
 */
public record UpdatePreleveur(
        @Nullable String name,
        @Nullable String phone,
        @Nullable String email,
        @Nullable String userId,
        @Nullable Set<Habilitation> habilitations) {}
