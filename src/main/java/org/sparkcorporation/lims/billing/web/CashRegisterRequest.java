package org.sparkcorporation.lims.billing.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/** Sealed parent for {@code /api/v1/cash-registers} create and update payloads (spec §7.3). */
public sealed interface CashRegisterRequest {

    /** {@code POST /api/v1/cash-registers} payload. */
    record Create(
            @NotBlank @Pattern(regexp = "^[A-Z0-9]{2}$") String code,
            @NotBlank @Size(max = 255) String label,
            @NotNull @Positive Integer sequenceNumber)
            implements CashRegisterRequest {}

    /** {@code PUT /api/v1/cash-registers/{id}} payload — code + sequenceNumber are immutable. */
    record Update(@Size(max = 255) @Nullable String label) implements CashRegisterRequest {}
}
