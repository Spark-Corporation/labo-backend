package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/** {@code code} is server-generated ({@code PRS-NNNNNN}); not part of the request. */
public record CreatePrescriberRequest(
        @Size(max = 10) @Nullable String title,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 26) @Nullable String specialtyId,
        @Size(max = 30) @Nullable String phone,
        @Email @Size(max = 120) @Nullable String email,
        @Size(max = 40) @Nullable String licenseNumber,
        @Size(max = 120) @Nullable String institution) {}
