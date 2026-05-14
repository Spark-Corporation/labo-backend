package org.sparkcorporation.lims.catalog.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.PartnerKind;

/** Null field = "leave unchanged" on the corresponding entity attribute. {@code code} immutable. */
public record UpdatePartner(
        @Nullable String name,
        @Nullable PartnerKind kind,
        @Nullable String responsibleEmail,
        @Nullable String responsibleContact,
        @Nullable BigDecimal bRate) {}
