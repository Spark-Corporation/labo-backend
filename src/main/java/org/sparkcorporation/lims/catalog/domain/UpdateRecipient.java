package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.MatrixId;

public record UpdateRecipient(
        @Nullable String name,
        @Nullable String description,
        @Nullable String color,
        @Nullable BigDecimal volumeMl,
        @Nullable MatrixId defaultMatrixId) {}
