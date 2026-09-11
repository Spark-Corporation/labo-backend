package org.sparkcorporation.lims.billing.web;

import java.time.Instant;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/** JSON response shape for {@code /api/v1/cash-registers} endpoints (spec §7.3). */
@Schema(description = "A cash register that payments can be recorded against.")
public record CashRegisterResponse(
        @Schema(description = "Cash register id.") String id,
        @Schema(description = "Short unique code for the register.") String code,
        @Schema(description = "Display label shown to cashiers.") String label,
        @Schema(
                        description =
                                "Sequence number used to generate this register's payment"
                                        + " receipt numbers.")
                Integer sequenceNumber,
        @Schema(
                        description =
                                "Ids of the users allowed to record payments through this"
                                        + " register.")
                Set<String> userIds,
        @Schema(description = "Whether the register has been disabled and can no longer be used.")
                boolean disabled,
        @Schema(description = "When the register was created.") Instant createdAt) {}
