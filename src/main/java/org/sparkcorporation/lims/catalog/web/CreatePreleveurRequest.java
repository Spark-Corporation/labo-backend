package org.sparkcorporation.lims.catalog.web;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.Habilitation;

/**
 * {@code code} is server-generated ({@code PHL-NNNNNN}); not part of the request. {@code
 * habilitations} must contain at least one entry (CDC §5).
 */
public record CreatePreleveurRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 30) @Nullable String phone,
        @Email @Size(max = 120) @Nullable String email,
        @Size(max = 26) @Nullable String userId,
        @NotNull @Size(min = 1) Set<Habilitation> habilitations) {}
