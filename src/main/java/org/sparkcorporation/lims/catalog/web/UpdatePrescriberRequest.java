package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/** PATCH semantics: all fields nullable; null = no change. {@code code} is server-managed. */
public record UpdatePrescriberRequest(
        @Size(max = 10) @Nullable String title,
        @Size(max = 120) @Nullable String name,
        @Size(max = 26) @Nullable String specialtyId,
        @Size(max = 30) @Nullable String phone,
        @Email @Size(max = 120) @Nullable String email,
        @Size(max = 40) @Nullable String licenseNumber,
        @Size(max = 120) @Nullable String institution) {}
