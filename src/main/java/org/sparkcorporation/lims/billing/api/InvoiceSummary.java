package org.sparkcorporation.lims.billing.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.order.api.DiscountKind;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Cross-module read snapshot of the {@link org.sparkcorporation.lims.billing.domain.Invoice}
 * aggregate. Carries the header columns plus a list of immutable {@link LineSummary} children.
 *
 * <p>Per spec §5.1 invariants: an invoice is immutable post-issuance — no update path exists. The
 * snapshot fields ({@code customerNameSnapshot}, {@code homeCollectionFeeSnapshot}, etc.) preserve
 * the customer-facing pricing context at issuance time regardless of subsequent catalog edits.
 */
public record InvoiceSummary(
        InvoiceId id,
        InvoiceNumber number,
        LabFileId labFileId,
        Instant issuedAt,
        InvoiceCustomerKind customerKind,
        String customerId,
        String customerNameSnapshot,
        BigDecimal homeCollectionFeeSnapshot,
        BigDecimal additionalFeesSnapshot,
        @Nullable DiscountKind discountKindSnapshot,
        @Nullable BigDecimal discountValueSnapshot,
        BigDecimal totalAmount,
        BigDecimal insuranceCoverageAmount,
        BigDecimal discountAmount,
        BigDecimal netAmount,
        List<LineSummary> lines) {

    public InvoiceSummary {
        lines = List.copyOf(lines);
    }

    /** One row per analysis priced on the invoice; positions are 1-based and contiguous. */
    public record LineSummary(
            String id,
            int position,
            AnalysisId analysisId,
            String analysisCodeSnapshot,
            String analysisNameSnapshot,
            BigDecimal indiciaryValueBSnapshot,
            BigDecimal bRateSnapshot,
            int labelCountSnapshot,
            BigDecimal lineTotal) {}
}
