package org.sparkcorporation.lims.result.infra.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportModel;

class DossierWordDocumentTest {

    // Column index of the "Résultat" cell in the analysis body table
    private static final int COL_RESULT_VALUE = 2;

    private final DossierWordDocument doc = new DossierWordDocument();

    private static ResultReportModel sampleModel() {
        return new ResultReportModel(
                new ResultReportModel.LabFileSummary(
                        "01HZX0000000000000000KBFE0",
                        "LF-260531-0001",
                        Instant.parse("2026-05-31T08:00:00Z")),
                new ResultReportModel.PatientSummary(
                        "01HZX00000000000000000PAT0", "Jane", "DOE", "F", "12/07/1985", "40 ans"),
                null,
                new ResultReportModel.LabIdentity("Lab Bio", "Lomé", "ACC-1", "Dr KOFFI"),
                Instant.parse("2026-05-31T10:00:00Z"),
                "31/05/2026 10:00",
                new ResultReportModel.UserSummary("01HZX00000000000000000USR0", "Dr KOFFI", ""),
                null,
                null,
                List.of(
                        new ResultReportModel.AnalysisGroupBlock(
                                "Biochimie",
                                List.of(
                                        new ResultReportModel.AnalysisRow(
                                                "GLY",
                                                "Glycémie",
                                                "1.45",
                                                "g/L",
                                                null,
                                                null,
                                                "0.70 - 1.10",
                                                null,
                                                true,
                                                null,
                                                null)))),
                List.of(),
                Locale.FRENCH);
    }

    /**
     * Model with two analysis rows in one group: one out-of-range (GLY) and one in-range (CAL).
     * Used to verify selective bold rendering of the result-value cell.
     */
    private static ResultReportModel twoRowModel() {
        return new ResultReportModel(
                new ResultReportModel.LabFileSummary(
                        "01HZX0000000000000000KBFE0",
                        "LF-260531-0001",
                        Instant.parse("2026-05-31T08:00:00Z")),
                new ResultReportModel.PatientSummary(
                        "01HZX00000000000000000PAT0", "Jane", "DOE", "F", "12/07/1985", "40 ans"),
                null,
                new ResultReportModel.LabIdentity("Lab Bio", "Lomé", "ACC-1", "Dr KOFFI"),
                Instant.parse("2026-05-31T10:00:00Z"),
                "31/05/2026 10:00",
                new ResultReportModel.UserSummary("01HZX00000000000000000USR0", "Dr KOFFI", ""),
                null,
                null,
                List.of(
                        new ResultReportModel.AnalysisGroupBlock(
                                "Biochimie",
                                List.of(
                                        new ResultReportModel.AnalysisRow(
                                                "GLY",
                                                "Glycémie",
                                                "1.45",
                                                "g/L",
                                                null,
                                                null,
                                                "0.70 - 1.10",
                                                null,
                                                true, // out-of-range: result value must be bold
                                                null,
                                                null),
                                        new ResultReportModel.AnalysisRow(
                                                "CAL",
                                                "Calcémie",
                                                "2.30",
                                                "mmol/L",
                                                null,
                                                null,
                                                "2.20 - 2.60",
                                                null,
                                                false, // in-range: result value must NOT be bold
                                                null,
                                                null)))),
                List.of(),
                Locale.FRENCH);
    }

    @Test
    void metadata_is_correct() {
        assertThat(doc.id()).isEqualTo("result.dossier");
        assertThat(doc.format()).isEqualTo(OfficeFormat.DOCX);
        assertThat(doc.modelClass()).isEqualTo(ResultReportModel.class);
        assertThat(doc.defaultFilename(sampleModel()))
                .isEqualTo("CR-01HZX0000000000000000KBFE0-20260531.docx");
    }

    @Test
    void build_produces_docx_with_key_fields() throws Exception {
        byte[] bytes = doc.build(sampleModel());
        try (var word = new XWPFDocument(new ByteArrayInputStream(bytes));
                var ex = new XWPFWordExtractor(word)) {
            String text = ex.getText();
            assertThat(text)
                    .contains("LF-260531-0001")
                    .contains("DOE")
                    .contains("Biochimie")
                    .contains("GLY")
                    .contains("1.45");
        }
    }

    /**
     * The result-value cell (column 2) of an out-of-range data row must be bold, and the
     * result-value cell of an in-range data row must NOT be bold. Tests the specific table cells
     * rather than scanning the whole document.
     *
     * <p>Table layout: row 0 = header (all cells bold unconditionally), rows 1+ = data. The
     * Biochimie group's table is the first table in the document.
     */
    @Test
    void out_of_range_result_value_cell_is_bold_and_in_range_is_not() throws Exception {
        byte[] bytes = doc.build(twoRowModel());
        try (var word = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            // The first table in the document is the Biochimie analysis table.
            var table = word.getTables().get(0);

            // Row 1 = GLY (out-of-range): result-value cell must be bold.
            XWPFTableCell outOfRangeValueCell = table.getRow(1).getCell(COL_RESULT_VALUE);
            boolean outOfRangeBold =
                    outOfRangeValueCell.getParagraphs().stream()
                            .flatMap(p -> p.getRuns().stream())
                            .anyMatch(r -> r.isBold());
            assertThat(outOfRangeBold)
                    .as("result-value cell of out-of-range row (GLY) should be bold")
                    .isTrue();

            // Row 2 = CAL (in-range): result-value cell must NOT be bold.
            XWPFTableCell inRangeValueCell = table.getRow(2).getCell(COL_RESULT_VALUE);
            boolean inRangeBold =
                    inRangeValueCell.getParagraphs().stream()
                            .flatMap(p -> p.getRuns().stream())
                            .anyMatch(r -> r.isBold());
            assertThat(inRangeBold)
                    .as("result-value cell of in-range row (CAL) should NOT be bold")
                    .isFalse();
        }
    }

    @Test
    void build_includes_comment_in_docx_when_non_null() throws Exception {
        var modelWithComment =
                new ResultReportModel(
                        new ResultReportModel.LabFileSummary(
                                "01HZX0000000000000000KBFE0",
                                "LF-260531-0001",
                                Instant.parse("2026-05-31T08:00:00Z")),
                        new ResultReportModel.PatientSummary(
                                "01HZX00000000000000000PAT0",
                                "Jane",
                                "DOE",
                                "F",
                                "12/07/1985",
                                "40 ans"),
                        null,
                        new ResultReportModel.LabIdentity("Lab Bio", "Lomé", "ACC-1", "Dr KOFFI"),
                        Instant.parse("2026-05-31T10:00:00Z"),
                        "31/05/2026 10:00",
                        new ResultReportModel.UserSummary(
                                "01HZX00000000000000000USR0", "Dr KOFFI", ""),
                        null,
                        null,
                        List.of(
                                new ResultReportModel.AnalysisGroupBlock(
                                        "Biochimie",
                                        List.of(
                                                new ResultReportModel.AnalysisRow(
                                                        "GLY",
                                                        "Glycémie",
                                                        "1.45",
                                                        "g/L",
                                                        null,
                                                        null,
                                                        "0.70 - 1.10",
                                                        null,
                                                        true,
                                                        "Résultat à contrôler",
                                                        null)))),
                        List.of(),
                        Locale.FRENCH);

        byte[] bytes = doc.build(modelWithComment);
        try (var word = new XWPFDocument(new ByteArrayInputStream(bytes));
                var ex = new XWPFWordExtractor(word)) {
            assertThat(ex.getText()).contains("Résultat à contrôler");
        }
    }
}
