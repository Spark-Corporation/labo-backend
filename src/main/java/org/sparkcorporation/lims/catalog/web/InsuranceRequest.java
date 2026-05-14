package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/** Sealed parent for {@code /api/v1/insurances} create and update payloads. */
public sealed interface InsuranceRequest {

    /** {@code POST /api/v1/insurances} payload — all required fields validated. */
    record Create(
            @NotBlank @Size(max = 8) String code,
            @NotBlank @Size(max = 255) String name,
            @Email @Size(max = 255) @Nullable String responsibleEmail,
            @Size(max = 50) @Nullable String responsibleContact,
            @NotNull @Positive BigDecimal bRate,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal coverageRate)
            implements InsuranceRequest {}

    /** {@code PUT /api/v1/insurances/{id}} payload — null fields mean "leave unchanged". */
    record Update(
            @Size(max = 255) @Nullable String name,
            @Email @Size(max = 255) @Nullable String responsibleEmail,
            @Size(max = 50) @Nullable String responsibleContact,
            @Positive @Nullable BigDecimal bRate,
            @DecimalMin("0.0") @DecimalMax("1.0") @Nullable BigDecimal coverageRate)
            implements InsuranceRequest {}
}
