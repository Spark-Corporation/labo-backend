package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@Schema(description = "An insurance payer covering a fraction of a patient's lab invoice.")
public record InsuranceResponse(
        @Schema(description = "ULID of the insurance.") String id,
        @Schema(description = "Admin-assigned insurance code.") String code,
        @Schema(description = "Display name of the insurance payer.") String name,
        @Schema(description = "Email address of the insurance's responsible contact.")
                @Nullable String responsibleEmail,
        @Schema(description = "Name or phone number of the insurance's responsible contact.")
                @Nullable String responsibleContact,
        @Schema(
                        description =
                                "Billing rate in XOF per indiciary point B; invoice amount billed"
                                        + " to this insurance = B x bRate x coverageRate.")
                BigDecimal bRate,
        @Schema(
                        description =
                                "Fraction (0 to 1) of the patient invoice this insurance covers;"
                                        + " the remainder stays payable by the patient.")
                BigDecimal coverageRate,
        @Schema(
                        description =
                                "Whether this insurance has been disabled (soft-deleted); disabled"
                                        + " insurances are excluded from active listings.")
                boolean disabled) {}
