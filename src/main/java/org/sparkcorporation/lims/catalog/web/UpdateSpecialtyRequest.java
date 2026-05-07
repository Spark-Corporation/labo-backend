package org.sparkcorporation.lims.catalog.web;

import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * PATCH semantics: all fields nullable; null = no change. {@code code} is immutable — the
 * controller rejects PATCH bodies that include a {@code code} field with 422 (spec §5.5).
 */
public record UpdateSpecialtyRequest(
        @Size(max = 120) @Nullable String name, @Size(max = 255) @Nullable String description) {}
