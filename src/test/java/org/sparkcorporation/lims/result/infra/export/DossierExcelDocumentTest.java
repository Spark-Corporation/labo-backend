package org.sparkcorporation.lims.result.infra.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportModel;

class DossierExcelDocumentTest {

    private final DossierExcelDocument doc = new DossierExcelDocument();

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
                                                "0.70 - 1.10",
                                                "g/L",
                                                true,
                                                null)))),
                List.of(),
                Locale.FRENCH);
    }

    @Test
    void metadata_is_correct() {
        assertThat(doc.id()).isEqualTo("result.dossier.xlsx");
        assertThat(doc.format()).isEqualTo(OfficeFormat.XLSX);
        assertThat(doc.modelClass()).isEqualTo(ResultReportModel.class);
        assertThat(doc.defaultFilename(sampleModel()))
                .isEqualTo("CR-01HZX0000000000000000KBFE0-20260531.xlsx");
    }

    @Test
    void build_produces_xlsx_with_resultats_sheet() throws Exception {
        byte[] bytes = doc.build(sampleModel());
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("Résultats");
            assertThat((Object) sheet).isNotNull();

            // Header row at row 0
            var headerRow = sheet.getRow(0);
            assertThat((Object) headerRow).isNotNull();
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Paillasse");

            // At least one data row with GLY, 1.45, g/L
            boolean hasGly = false;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row != null && "GLY".equals(row.getCell(1).getStringCellValue())) {
                    hasGly = true;
                    assertThat(row.getCell(3).getStringCellValue()).isEqualTo("1.45");
                    assertThat(row.getCell(5).getStringCellValue()).isEqualTo("g/L");
                }
            }
            assertThat(hasGly).as("row with code GLY should be present").isTrue();
        }
    }

    @Test
    void build_includes_dossier_info_sheet() throws Exception {
        byte[] bytes = doc.build(sampleModel());
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var infoSheet = wb.getSheet("Dossier");
            assertThat((Object) infoSheet).isNotNull();

            // The info sheet should contain the dailyId and patient last name
            boolean hasDailyId = false;
            boolean hasPatientName = false;
            for (int i = 0; i <= infoSheet.getLastRowNum(); i++) {
                var row = infoSheet.getRow(i);
                if (row != null) {
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        var cell = row.getCell(j);
                        if (cell != null) {
                            String val = cell.getStringCellValue();
                            if (val.contains("LF-260531-0001")) hasDailyId = true;
                            if (val.contains("DOE")) hasPatientName = true;
                        }
                    }
                }
            }
            assertThat(hasDailyId).as("Dossier sheet should contain daily ID").isTrue();
            assertThat(hasPatientName)
                    .as("Dossier sheet should contain patient last name")
                    .isTrue();
        }
    }
}
