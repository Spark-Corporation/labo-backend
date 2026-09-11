package org.sparkcorporation.lims.billing.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Sealed parent for {@code /api/v1/cash-registers} create and update payloads (spec §7.3). */
public sealed interface CashRegisterRequest {

    /** {@code POST /api/v1/cash-registers} payload. */
    @Schema(
            description =
                    "Creates a physical/logical cash register that payments are recorded"
                            + " against.")
    record Create(
            @Schema(
                            description =
                                    "Short unique code for the register, two uppercase letters"
                                            + " and/or digits (e.g. \"C1\").")
                    @NotBlank
                    @Pattern(regexp = "^[A-Z0-9]{2}$")
                    String code,
            @Schema(description = "Display label shown to cashiers when selecting the register.")
                    @NotBlank
                    @Size(max = 255)
                    String label,
            @Schema(
                            description =
                                    "Sequence number used to generate this register's payment"
                                            + " receipt numbers.")
                    @NotNull
                    @Positive
                    Integer sequenceNumber)
            implements CashRegisterRequest {}

    /** {@code PUT /api/v1/cash-registers/{id}} payload — code + sequenceNumber are immutable. */
    @Schema(
            description =
                    "Renames a cash register. code and sequenceNumber cannot be changed after"
                            + " creation.")
    record Update(
            @Schema(description = "New display label; omit/null leaves it unchanged.")
                    @Size(max = 255)
                    @Nullable String label)
            implements CashRegisterRequest {}
}
