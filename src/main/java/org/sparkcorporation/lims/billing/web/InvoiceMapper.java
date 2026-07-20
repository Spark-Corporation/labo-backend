package org.sparkcorporation.lims.billing.web;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.billing.api.InvoiceSummary;

/** Maps {@link InvoiceSummary} → {@link InvoiceResponse} JSON DTO. */
@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(InvoiceSummary s) {
        var lines =
                s.lines().stream()
                        .map(
                                l ->
                                        new InvoiceResponse.LineResponse(
                                                l.id(),
                                                l.position(),
                                                l.analysisId().value(),
                                                l.analysisCodeSnapshot(),
                                                l.analysisNameSnapshot(),
                                                l.indiciaryValueBSnapshot(),
                                                l.bRateSnapshot(),
                                                l.labelCountSnapshot(),
                                                l.lineTotal()))
                        .toList();
        return new InvoiceResponse(
                s.id().value(),
                s.number().value(),
                s.labFileId().value(),
                s.issuedAt(),
                s.customerKind(),
                s.customerId(),
                s.customerNameSnapshot(),
                s.homeCollectionFeeSnapshot(),
                s.additionalFeesSnapshot(),
                s.discountKindSnapshot(),
                s.discountValueSnapshot(),
                s.totalAmount(),
                s.insuranceCoverageAmount(),
                s.discountAmount(),
                s.netAmount(),
                lines);
    }
}
