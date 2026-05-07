package org.sparkcorporation.lims.catalog.web;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.Habilitation;

/**
 * PATCH semantics: all fields nullable; null = no change. {@code code} is server-managed. {@code
 * habilitations}: null = no change; non-null empty = service-layer 422 ({@code
 * MissingHabilitationException}); non-null non-empty = full replace.
 */
public record UpdatePreleveurRequest(
        @Size(max = 120) @Nullable String name,
        @Size(max = 30) @Nullable String phone,
        @Email @Size(max = 120) @Nullable String email,
        @Size(max = 26) @Nullable String userId,
        @Nullable Set<Habilitation> habilitations) {}
