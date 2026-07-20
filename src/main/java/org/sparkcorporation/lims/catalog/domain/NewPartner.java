package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.PartnerKind;

public record NewPartner(
        String code,
        String name,
        PartnerKind kind,
        @Nullable String responsibleEmail,
        @Nullable String responsibleContact,
        BigDecimal bRate) {}
