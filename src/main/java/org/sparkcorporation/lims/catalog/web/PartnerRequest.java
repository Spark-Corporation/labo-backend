package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.PartnerKind;

public sealed interface PartnerRequest {

    @Schema(
            description =
                    "Request to create a new corporate/institutional partner that bills the entire"
                            + " patient invoice.")
    record Create(
            @Schema(description = "Admin-assigned partner code; immutable after creation.")
                    @NotBlank
                    @Size(max = 8)
                    String code,
            @Schema(description = "Display name of the partner.") @NotBlank @Size(max = 255)
                    String name,
            @Schema(description = "Partner kind: SOCIETY, PARTNER_LAB or HEALTH_CENTER.") @NotNull
                    PartnerKind kind,
            @Schema(description = "Email address of the partner's responsible contact.")
                    @Email
                    @Size(max = 255)
                    @Nullable String responsibleEmail,
            @Schema(description = "Name or phone number of the partner's responsible contact.")
                    @Size(max = 50)
                    @Nullable String responsibleContact,
            @Schema(
                            description =
                                    "Billing rate in XOF per indiciary point B; invoice amount"
                                        + " billed to this partner = B x bRate (the partner pays"
                                        + " the full invoice).")
                    @NotNull
                    @Positive
                    BigDecimal bRate)
            implements PartnerRequest {}

    @Schema(description = "Request to update a partner; null fields are left unchanged.")
    record Update(
            @Schema(description = "Display name of the partner.") @Size(max = 255)
                    @Nullable String name,
            @Schema(description = "Partner kind: SOCIETY, PARTNER_LAB or HEALTH_CENTER.")
                    @Nullable PartnerKind kind,
            @Schema(description = "Email address of the partner's responsible contact.")
                    @Email
                    @Size(max = 255)
                    @Nullable String responsibleEmail,
            @Schema(description = "Name or phone number of the partner's responsible contact.")
                    @Size(max = 50)
                    @Nullable String responsibleContact,
            @Schema(
                            description =
                                    "Billing rate in XOF per indiciary point B; invoice amount"
                                        + " billed to this partner = B x bRate (the partner pays"
                                        + " the full invoice).")
                    @Positive
                    @Nullable BigDecimal bRate)
            implements PartnerRequest {}
}
