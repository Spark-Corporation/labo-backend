package org.sparkcorporation.lims.billing.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.order.api.DiscountKind;

/** JSON response shape for {@code Invoice} endpoints (spec §7.2). */
public record InvoiceResponse(
        String id,
        String number,
        String labFileId,
        Instant issuedAt,
        InvoiceCustomerKind customerKind,
        String customerId,
        String customerName,
        BigDecimal homeCollectionFee,
        BigDecimal additionalFees,
        @Nullable DiscountKind discountKind,
        @Nullable BigDecimal discountValue,
        BigDecimal totalAmount,
        BigDecimal insuranceCoverageAmount,
        BigDecimal discountAmount,
        BigDecimal netAmount,
        List<LineResponse> lines) {

    public record LineResponse(
            String id,
            int position,
            String analysisId,
            String analysisCode,
            String analysisName,
            BigDecimal indiciaryValueB,
            BigDecimal bRate,
            int labelCount,
            BigDecimal lineTotal) {}
}
