package org.sparkcorporation.lims.order.infra.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportModel;

class QuoteWordDocumentTest {

    private final QuoteWordDocument doc = new QuoteWordDocument();

    private static QuoteReportModel sampleModel() {
        return new QuoteReportModel(
                new QuoteReportModel.QuoteHeader(
                        "01HZX0000000000000000QUOTE",
                        "DEV-260531-0001",
                        "31/05/2026",
                        Instant.parse("2026-05-31T09:00:00Z"),
                        null,
                        null,
                        null,
                        QuoteStatus.DRAFT),
                new QuoteReportModel.PatientSummary("DOE", "Jane", "F", "LAB-0001"),
                null,
                new QuoteReportModel.StatusBanner("Devis", "non contractuel", true),
                List.of(
                        new QuoteReportModel.LineRow(
                                "NFS", "Numération", "B 30", 1, "3 000 F CFA")),
                new QuoteReportModel.TotalsBlock("3 000 F CFA"),
                new QuoteReportModel.LabIdentity("Lab Bio", "Lomé", "ACC-1", "Dr KOFFI"),
                Locale.FRENCH);
    }

    @Test
    void metadata_is_correct() {
        assertThat(doc.id()).isEqualTo("order.quote");
        assertThat(doc.format()).isEqualTo(OfficeFormat.DOCX);
        assertThat(doc.modelClass()).isEqualTo(QuoteReportModel.class);
        assertThat(doc.defaultFilename(sampleModel()))
                .isEqualTo("DEVIS-DEV-260531-0001-20260531.docx");
    }

    @Test
    void build_produces_a_docx_with_the_key_fields() throws Exception {
        byte[] bytes = doc.build(sampleModel());
        try (var word = new XWPFDocument(new ByteArrayInputStream(bytes));
                var ex = new XWPFWordExtractor(word)) {
            String text = ex.getText();
            assertThat(text)
                    .contains("DEV-260531-0001")
                    .contains("DOE")
                    .contains("Jane")
                    .contains("NFS")
                    .contains("3 000 F CFA");
        }
    }
}
