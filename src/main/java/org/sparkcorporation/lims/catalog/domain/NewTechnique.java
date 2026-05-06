package org.sparkcorporation.lims.catalog.domain;

import org.jspecify.annotations.Nullable;

public record NewTechnique(String code, String name, @Nullable String description) {}
