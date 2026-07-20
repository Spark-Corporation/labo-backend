package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/** Null field = "leave unchanged" on the corresponding entity attribute. */
public record UpdateInsurance(
        @Nullable String name,
        @Nullable String responsibleEmail,
        @Nullable String responsibleContact,
        @Nullable BigDecimal bRate,
        @Nullable BigDecimal coverageRate) {}
