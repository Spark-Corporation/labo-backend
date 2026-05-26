package org.sparkcorporation.lims.order.infra.pdf;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.pdf.api.PageSize;
import org.sparkcorporation.lims.pdf.api.PdfDocumentType;

/**
 * {@link PdfDocumentType} for the quote (devis) PDF (spec 1.5.h).
 *
 * <p>FFT: {@code QuoteReportModelAssemblerTest.assemble_LOCKED_populatesPricingFields}.
 */
@Component
public class QuoteReportPdfDocument implements PdfDocumentType<QuoteReportModel> {

    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    @Override
    public String id() {
        return "order.quote-report";
    }

    @Override
    public String templatePath() {
        return "order/quote-report";
    }

    @Override
    public Class<QuoteReportModel> modelClass() {
        return QuoteReportModel.class;
    }

    @Override
    public PageSize pageSize() {
        return PageSize.A4_PORTRAIT;
    }

    @Override
    public String defaultFilename(QuoteReportModel model) {
        return "DEVIS-%s-%s.pdf"
                .formatted(
                        model.header().quoteNumber(),
                        FILE_DATE.format(model.header().createdAtUtc()));
    }
}
