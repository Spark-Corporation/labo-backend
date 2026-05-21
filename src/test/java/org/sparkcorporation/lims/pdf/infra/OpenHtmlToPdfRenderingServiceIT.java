package org.sparkcorporation.lims.pdf.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.pdf.api.PdfDocumentType;
import org.sparkcorporation.lims.pdf.api.PdfRenderingService;
import org.sparkcorporation.lims.pdf.api.PdfTemplateException;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportModel;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportPdfDocument;

@SpringBootTest
class OpenHtmlToPdfRenderingServiceIT extends IntegrationTestBase {

    @Autowired private PdfRenderingService pdfService;
    @Autowired private TestPdfDocumentType testDocumentType;
    @Autowired private ResultReportPdfDocument resultReportDoc;

    public record TestModel(String id, String marker) {}

    @TestConfiguration
    static class Config {
        @Bean
        TestPdfDocumentType testPdfDocumentType() {
            return new TestPdfDocumentType();
        }
    }

    public static class TestPdfDocumentType implements PdfDocumentType<TestModel> {
        @Override
        public String id() {
            return "test.engine-smoke";
        }

        @Override
        public String templatePath() {
            return "test/test-doc";
        }

        @Override
        public Class<TestModel> modelClass() {
            return TestModel.class;
        }

        @Override
        public String defaultFilename(TestModel m) {
            return "test-" + m.id() + ".pdf";
        }
    }

    @Test
    void syncRenderReturnsPdfBytesForTestDocumentType() throws Exception {
        var model = new TestModel("01HZX0TEST0000000000000000", "smoke-marker-XYZ");

        var rendered = pdfService.render(testDocumentType, model);

        assertThat(rendered.bytes()).isNotEmpty();
        assertThat(rendered.bytes()[0]).isEqualTo((byte) '%');
        assertThat(rendered.bytes()[1]).isEqualTo((byte) 'P');
        assertThat(rendered.bytes()[2]).isEqualTo((byte) 'D');
        assertThat(rendered.bytes()[3]).isEqualTo((byte) 'F');
        assertThat(rendered.suggestedFilename()).isEqualTo("test-01HZX0TEST0000000000000000.pdf");

        try (var doc = PDDocument.load(rendered.bytes())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            var text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Engine smoke");
            assertThat(text).contains("01HZX0TEST0000000000000000");
            assertThat(text).contains("smoke-marker-XYZ");
        }
    }

    @Test
    void unknownTemplatePathProducesPdfTemplateException() {
        var brokenType =
                new PdfDocumentType<TestModel>() {
                    @Override
                    public String id() {
                        return "test.broken";
                    }

                    @Override
                    public String templatePath() {
                        return "does/not/exist";
                    }

                    @Override
                    public Class<TestModel> modelClass() {
                        return TestModel.class;
                    }

                    @Override
                    public String defaultFilename(TestModel m) {
                        return "broken.pdf";
                    }
                };

        assertThatExceptionOfType(PdfTemplateException.class)
                .isThrownBy(() -> pdfService.render(brokenType, new TestModel("x", "y")))
                .matches(
                        ex ->
                                "template_not_found".equals(ex.reasonTag())
                                        || "template_parse_error".equals(ex.reasonTag()));
    }

    @Test
    void render_returnsPdfBytesForResultReport() throws Exception {
        var model = stubResultReportModel();

        var rendered = pdfService.render(resultReportDoc, model);

        assertThat(rendered.bytes()).isNotEmpty();
        assertThat(rendered.bytes()[0]).isEqualTo((byte) '%');

        try (var doc = PDDocument.load(rendered.bytes())) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            var text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Résultats");
            assertThat(text).contains("Compte-rendu");
            assertThat(text).contains(model.labFile().labFileId());
            for (var group : model.groups()) {
                for (var row : group.rows()) {
                    assertThat(text).contains(row.code());
                }
            }
        }
    }

    private static ResultReportModel stubResultReportModel() {
        var labFileId = "01HZB0000000000000000KBFE0";
        var biochimieRows =
                java.util.List.of(
                        new ResultReportModel.AnalysisRow(
                                "GLU", "Glucose", "1.05", "0.7-1.10", "g/L", false, null),
                        new ResultReportModel.AnalysisRow(
                                "CRE",
                                "Créatinine",
                                "12.4",
                                "6-12",
                                "mg/L",
                                true,
                                "À recontrôler"));
        var hemaRows =
                java.util.List.of(
                        new ResultReportModel.AnalysisRow(
                                "HB", "Hémoglobine", "13.5", "12-16", "g/dL", false, null),
                        new ResultReportModel.AnalysisRow(
                                "GR", "Globules rouges", "4.6", "4.0-5.5", "T/L", false, null));
        return new ResultReportModel(
                new ResultReportModel.LabFileSummary(
                        labFileId,
                        "LF-260520-9001",
                        java.time.Instant.parse("2026-05-20T08:00:00Z")),
                new ResultReportModel.PatientSummary(
                        "01HZB0000000000000000PATE0", "Jane", "DOE", "F", "01/01/1985", "41 ans"),
                null,
                new ResultReportModel.LabIdentity(
                        "Laboratoire LIMS Test", "Lomé, Togo", "ACC-2026-001", "Dr. Directeur"),
                java.time.Instant.parse("2026-05-20T12:30:00Z"),
                "20/05/2026 12:30",
                new ResultReportModel.UserSummary(
                        "01HZB000000000000000BGST00", "Dr. KOFFI Adjoa", "Biologiste"),
                java.util.List.of(
                        new ResultReportModel.AnalysisGroupBlock("Biochimie", biochimieRows),
                        new ResultReportModel.AnalysisGroupBlock("Hématologie", hemaRows)),
                java.util.List.of(),
                java.util.Locale.FRENCH);
    }
}
