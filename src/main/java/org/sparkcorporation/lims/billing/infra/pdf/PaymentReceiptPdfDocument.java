package org.sparkcorporation.lims.billing.infra.pdf;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.pdf.api.PageSize;
import org.sparkcorporation.lims.pdf.api.PdfDocumentType;

/**
 * {@link PdfDocumentType} for the payment receipt PDF (spec 1.5.g). Lives under {@code
 * billing.infra.pdf} per the chunk-7 ArchUnit convention from 1.5.c.
 *
 * <p>Spec FFT: {@code PaymentReceiptAcceptanceIT.GET_returns200_andPdfContainsExpectedText}.
 */
@Component
public class PaymentReceiptPdfDocument implements PdfDocumentType<PaymentReceiptModel> {

    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    @Override
    public String id() {
        return "billing.payment-receipt";
    }

    @Override
    public String templatePath() {
        return "billing/payment-receipt";
    }

    @Override
    public Class<PaymentReceiptModel> modelClass() {
        return PaymentReceiptModel.class;
    }

    @Override
    public PageSize pageSize() {
        return PageSize.A5_PORTRAIT;
    }

    @Override
    public String defaultFilename(PaymentReceiptModel model) {
        return "RECU-%s-%s.pdf"
                .formatted(
                        model.labFile().labFileId(),
                        FILE_DATE.format(model.header().issuedAtUtc()));
    }
}
