package org.sparkcorporation.lims.catalog.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Sealed parent for {@code /api/v1/insurances} create and update payloads. */
public sealed interface InsuranceRequest {

    /** {@code POST /api/v1/insurances} payload — all required fields validated. */
    @Schema(description = "Request to create a new insurance payer.")
    record Create(
            @Schema(description = "Admin-assigned insurance code; immutable after creation.")
                    @NotBlank
                    @Size(max = 8)
                    String code,
            @Schema(description = "Display name of the insurance payer.") @NotBlank @Size(max = 255)
                    String name,
            @Schema(description = "Email address of the insurance's responsible contact.")
                    @Email
                    @Size(max = 255)
                    @Nullable String responsibleEmail,
            @Schema(description = "Name or phone number of the insurance's responsible contact.")
                    @Size(max = 50)
                    @Nullable String responsibleContact,
            @Schema(
                            description =
                                    "Billing rate in XOF per indiciary point B; invoice amount"
                                        + " billed to this insurance = B x bRate x coverageRate.")
                    @NotNull
                    @Positive
                    BigDecimal bRate,
            @Schema(
                            description =
                                    "Fraction (0 to 1) of the patient invoice this insurance"
                                        + " covers; the remainder stays payable by the patient.")
                    @NotNull
                    @DecimalMin("0.0")
                    @DecimalMax("1.0")
                    BigDecimal coverageRate)
            implements InsuranceRequest {}

    /** {@code PUT /api/v1/insurances/{id}} payload — null fields mean "leave unchanged". */
    @Schema(description = "Request to update an insurance payer; null fields are left unchanged.")
    record Update(
            @Schema(description = "Display name of the insurance payer.") @Size(max = 255)
                    @Nullable String name,
            @Schema(description = "Email address of the insurance's responsible contact.")
                    @Email
                    @Size(max = 255)
                    @Nullable String responsibleEmail,
            @Schema(description = "Name or phone number of the insurance's responsible contact.")
                    @Size(max = 50)
                    @Nullable String responsibleContact,
            @Schema(
                            description =
                                    "Billing rate in XOF per indiciary point B; invoice amount"
                                        + " billed to this insurance = B x bRate x coverageRate.")
                    @Positive
                    @Nullable BigDecimal bRate,
            @Schema(
                            description =
                                    "Fraction (0 to 1) of the patient invoice this insurance"
                                        + " covers; the remainder stays payable by the patient.")
                    @DecimalMin("0.0")
                    @DecimalMax("1.0")
                    @Nullable BigDecimal coverageRate)
            implements InsuranceRequest {}
}
