package org.sparkcorporation.lims.catalog.domain;

import org.jspecify.annotations.Nullable;

/** All fields nullable; null = no change. Code is NOT modifiable. */
public record UpdateTechnique(@Nullable String name, @Nullable String description) {}
