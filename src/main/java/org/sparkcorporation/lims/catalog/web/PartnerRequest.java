package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.PartnerKind;

public sealed interface PartnerRequest {

    record Create(
            @NotBlank @Size(max = 8) String code,
            @NotBlank @Size(max = 255) String name,
            @NotNull PartnerKind kind,
            @Email @Size(max = 255) @Nullable String responsibleEmail,
            @Size(max = 50) @Nullable String responsibleContact,
            @NotNull @Positive BigDecimal bRate)
            implements PartnerRequest {}

    record Update(
            @Size(max = 255) @Nullable String name,
            @Nullable PartnerKind kind,
            @Email @Size(max = 255) @Nullable String responsibleEmail,
            @Size(max = 50) @Nullable String responsibleContact,
            @Positive @Nullable BigDecimal bRate)
            implements PartnerRequest {}
}
