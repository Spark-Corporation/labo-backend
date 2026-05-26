package org.sparkcorporation.lims.order.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.pdf.api.PageSize;

class QuoteReportPdfDocumentTest {

    private final QuoteReportPdfDocument doc = new QuoteReportPdfDocument();

    @Test
    void id_is_order_quote_report() {
        assertThat(doc.id()).isEqualTo("order.quote-report");
    }

    @Test
    void templatePath_is_order_quote_report() {
        assertThat(doc.templatePath()).isEqualTo("order/quote-report");
    }

    @Test
    void modelClass_is_QuoteReportModel() {
        assertThat(doc.modelClass()).isEqualTo(QuoteReportModel.class);
    }

    @Test
    void pageSize_is_A4_PORTRAIT() {
        assertThat(doc.pageSize()).isEqualTo(PageSize.A4_PORTRAIT);
    }

    @Test
    void defaultFilename_uses_quoteNumber_and_yyyyMMdd_from_createdAtUtc() {
        var model =
                new QuoteReportModel(
                        new QuoteReportModel.QuoteHeader(
                                "01HZX00000000000000QV0TE00",
                                "DEV-260526-0001",
                                "26/05/2026",
                                Instant.parse("2026-05-26T08:00:00Z"),
                                null,
                                null,
                                null,
                                QuoteStatus.DRAFT),
                        new QuoteReportModel.PatientSummary("DOE", "Jane", "F", "PAT-260526-0001"),
                        null,
                        new QuoteReportModel.StatusBanner("BROUILLON", "non contractuel", true),
                        List.of(),
                        new QuoteReportModel.TotalsBlock("À déterminer"),
                        new QuoteReportModel.LabIdentity("Lab", "Addr", "ACC", "Mgr"),
                        Locale.FRENCH);

        assertThat(doc.defaultFilename(model)).isEqualTo("DEVIS-DEV-260526-0001-20260526.pdf");
    }
}
