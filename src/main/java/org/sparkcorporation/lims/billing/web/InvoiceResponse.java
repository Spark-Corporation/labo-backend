package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.order.api.DiscountKind;

/** JSON response shape for {@code Invoice} endpoints (spec §7.2). */
@Schema(
        description =
                "An issued invoice: the billed analyses, the applicable discount and insurance "
                        + "coverage, and the resulting amounts owed.")
public record InvoiceResponse(
        @Schema(description = "Invoice id.") String id,
        @Schema(description = "Human-readable invoice number, unique per lab.") String number,
        @Schema(description = "Id of the lab file this invoice was issued for.") String labFileId,
        @Schema(description = "When the invoice was issued.") Instant issuedAt,
        @Schema(
                        description =
                                "Who is billed for this invoice: the patient, their insurer, or a"
                                        + " partner account.")
                InvoiceCustomerKind customerKind,
        @Schema(description = "Id of the billed party, whose kind is given by customerKind.")
                String customerId,
        @Schema(description = "Display name of the billed party, captured at issuance time.")
                String customerName,
        @Schema(description = "Home-sample-collection fee included in this invoice, in XOF.")
                BigDecimal homeCollectionFee,
        @Schema(description = "Other additional fees included in this invoice, in XOF.")
                BigDecimal additionalFees,
        @Nullable
                @Schema(
                        description =
                                "Whether the discount below is a flat amount or a percentage;"
                                        + " absent when no discount applies.")
                DiscountKind discountKind,
        @Nullable
                @Schema(
                        description =
                                "The discount's raw value: an XOF amount when discountKind is"
                                        + " AMOUNT, or a fraction in [0, 1] when it is PERCENT.")
                BigDecimal discountValue,
        @Schema(
                        description =
                                "Sum of the line totals plus fees, before discount and insurance"
                                        + " coverage, in XOF.")
                BigDecimal totalAmount,
        @Schema(
                        description =
                                "Portion of totalAmount covered by the patient's insurance, in"
                                        + " XOF.")
                BigDecimal insuranceCoverageAmount,
        @Schema(
                        description =
                                "Discount amount actually applied, in XOF (derived from"
                                        + " discountKind/discountValue).")
                BigDecimal discountAmount,
        @Schema(
                        description =
                                "Amount the customer owes: totalAmount minus"
                                        + " insuranceCoverageAmount and discountAmount, in XOF.")
                BigDecimal netAmount,
        @Schema(description = "One line per billed analysis.") List<LineResponse> lines) {

    @Schema(description = "One billed analysis within an invoice.")
    public record LineResponse(
            @Schema(description = "Invoice line id.") String id,
            @Schema(description = "1-based position of this line on the invoice.") int position,
            @Schema(description = "Id of the billed analysis.") String analysisId,
            @Schema(description = "Code of the billed analysis, as of issuance time.")
                    String analysisCode,
            @Schema(description = "Name of the billed analysis, as of issuance time.")
                    String analysisName,
            @Schema(
                            description =
                                    "Analysis's indiciary value B (nomenclature coefficient) used"
                                            + " to price this line.")
                    BigDecimal indiciaryValueB,
            @Schema(description = "Monetary value of one unit of B at issuance time, in XOF.")
                    BigDecimal bRate,
            @Schema(description = "Number of result labels billed for this line.") int labelCount,
            @Schema(description = "Line amount: indiciaryValueB * bRate * labelCount, in XOF.")
                    BigDecimal lineTotal) {}
}
