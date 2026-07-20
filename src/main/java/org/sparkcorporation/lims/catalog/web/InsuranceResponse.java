package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record InsuranceResponse(
        String id,
        String code,
        String name,
        @Nullable String responsibleEmail,
        @Nullable String responsibleContact,
        BigDecimal bRate,
        BigDecimal coverageRate,
        boolean disabled) {}
