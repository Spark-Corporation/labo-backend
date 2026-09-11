package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.PartnerKind;

@Schema(
        description =
                "A corporate or institutional partner that bills the entire patient invoice (no"
                        + " patient share), unlike an insurance.")
public record PartnerResponse(
        @Schema(description = "ULID of the partner.") String id,
        @Schema(description = "Admin-assigned partner code.") String code,
        @Schema(description = "Display name of the partner.") String name,
        @Schema(description = "Partner kind: SOCIETY, PARTNER_LAB or HEALTH_CENTER.")
                PartnerKind kind,
        @Schema(description = "Email address of the partner's responsible contact.")
                @Nullable String responsibleEmail,
        @Schema(description = "Name or phone number of the partner's responsible contact.")
                @Nullable String responsibleContact,
        @Schema(
                        description =
                                "Billing rate in XOF per indiciary point B; invoice amount billed"
                                        + " to this partner = B x bRate (the partner pays the full"
                                        + " invoice).")
                BigDecimal bRate,
        @Schema(
                        description =
                                "Whether this partner has been disabled (soft-deleted); disabled"
                                        + " partners are excluded from active listings.")
                boolean disabled) {}
