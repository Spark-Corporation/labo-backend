package org.sparkcorporation.lims.catalog.domain;

import org.jspecify.annotations.Nullable;

public record NewSpecialty(String code, String name, @Nullable String description) {}
