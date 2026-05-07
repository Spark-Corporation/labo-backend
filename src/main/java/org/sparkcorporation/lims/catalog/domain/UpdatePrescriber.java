package org.sparkcorporation.lims.catalog.domain;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.SpecialtyId;

/** All fields nullable; null = no change. Code is NOT modifiable (server-managed). */
public record UpdatePrescriber(
        @Nullable String title,
        @Nullable String name,
        @Nullable SpecialtyId specialtyId,
        @Nullable String phone,
        @Nullable String email,
        @Nullable String licenseNumber,
        @Nullable String institution) {}
