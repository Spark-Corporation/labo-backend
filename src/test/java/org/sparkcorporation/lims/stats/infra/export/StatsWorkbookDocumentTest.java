package org.sparkcorporation.lims.stats.infra.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.stats.infra.ActivityRow;
import org.sparkcorporation.lims.stats.infra.AnalysisStatRow;
import org.sparkcorporation.lims.stats.infra.AntibiogramStatRow;
import org.sparkcorporation.lims.stats.infra.DisciplineStatRow;
import org.sparkcorporation.lims.stats.infra.PatientStatsRow;
import org.sparkcorporation.lims.stats.infra.PrescriberStatRow;

class StatsWorkbookDocumentTest {

    private final StatsWorkbookDocument doc = new StatsWorkbookDocument();

    private static final LocalDate FROM = LocalDate.of(2026, 5, 1);
    private static final LocalDate TO = LocalDate.of(2026, 5, 31);

    private static StatsWorkbookModel sampleModel() {
        return new StatsWorkbookModel(
                FROM,
                TO,
                new ActivityRow(10, 2, 8, 9, 3),
                List.of(
                        new DisciplineStatRow(
                                "01DISC0000000000000000BIOC", "BIOCHIMIE", "Biochimie", 50L, 5L)),
                List.of(
                        new PrescriberStatRow(
                                "01PRSC0000000000000000DOC1", "PR-001", "Dr Koffi", 10L)),
                List.of(
                        new AnalysisStatRow(
                                "01ANLZ0000000000000000GLY0", "GLY", "Glycémie", 30L, 25L, 3L, 2L)),
                List.of(
                        new AntibiogramStatRow(
                                "01ANTI0000000000000000AMX0", "AMX", "Amoxicilline", 10L, 2L, 3L)),
                new PatientStatsRow(9L, 3L, Map.of("M", 5L, "F", 4L), Map.of("ADULTE", 7L)));
    }

    @Test
    void metadata_is_correct() {
        var model = sampleModel();
        assertThat(doc.id()).isEqualTo("stats.workbook");
        assertThat(doc.format()).isEqualTo(OfficeFormat.XLSX);
        assertThat(doc.modelClass()).isEqualTo(StatsWorkbookModel.class);
        assertThat(doc.defaultFilename(model)).isEqualTo("STATISTIQUES-2026-05-01-2026-05-31.xlsx");
    }

    @Test
    void build_produces_xlsx_with_six_named_sheets() throws Exception {
        byte[] bytes = doc.build(sampleModel());
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(6);
            assertThat(wb.getSheet("Activité")).isNotNull();
            assertThat(wb.getSheet("Par discipline")).isNotNull();
            assertThat(wb.getSheet("Par prescripteur")).isNotNull();
            assertThat(wb.getSheet("Analyses")).isNotNull();
            assertThat(wb.getSheet("Antibiogrammes")).isNotNull();
            assertThat(wb.getSheet("Patients")).isNotNull();
        }
    }

    @Test
    void activity_sheet_contains_dossier_count() throws Exception {
        byte[] bytes = doc.build(sampleModel());
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("Activité");
            // Find a cell with the numeric value 10 (dossiers count)
            boolean foundDossiers = false;
            for (var row : sheet) {
                for (var cell : row) {
                    if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                            && (long) cell.getNumericCellValue() == 10L) {
                        foundDossiers = true;
                    }
                }
            }
            assertThat(foundDossiers).isTrue();
        }
    }

    @Test
    void discipline_sheet_contains_header_and_data_row() throws Exception {
        byte[] bytes = doc.build(sampleModel());
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("Par discipline");
            assertThat(sheet.getPhysicalNumberOfRows()).isGreaterThanOrEqualTo(2);
            // Row 0 = header, row 1 = first data row with "BIOCHIMIE"
            String code = sheet.getRow(1).getCell(0).getStringCellValue();
            assertThat(code).isEqualTo("BIOCHIMIE");
        }
    }

    @Test
    void analyses_sheet_has_data() throws Exception {
        byte[] bytes = doc.build(sampleModel());
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("Analyses");
            assertThat(sheet.getPhysicalNumberOfRows()).isGreaterThanOrEqualTo(2);
            // Row 1, cell 0 = code "GLY"
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("GLY");
        }
    }
}
