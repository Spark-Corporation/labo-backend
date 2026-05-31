package org.sparkcorporation.lims.result.infra.export;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.export.api.OfficeDocumentType;
import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.export.api.OfficeRenderException;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportModel;

/**
 * {@link OfficeDocumentType} for the result dossier Excel export (CDC §IV). Password-protected; the
 * controller generates and returns the password in {@code X-Export-Password}.
 *
 * <p>Produces a workbook with two sheets:
 *
 * <ul>
 *   <li>{@code Dossier} — identification block (lab file ID, patient summary, biologist, dates).
 *   <li>{@code Résultats} — one row per analysis result line, flattened across paillasse groups
 *       (the paillasse name appears as the first column so discipline grouping survives in tabular
 *       form).
 * </ul>
 */
@Component
public class DossierExcelDocument implements OfficeDocumentType<ResultReportModel> {

    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    // Column indices for the Résultats sheet
    private static final int COL_PAILLASSE = 0;
    private static final int COL_CODE = 1;
    private static final int COL_LIBELLE = 2;
    private static final int COL_RESULT = 3;
    private static final int COL_NORMES = 4;
    private static final int COL_UNIT = 5;
    private static final int COL_OUT_OF_RANGE = 6;

    @Override
    public String id() {
        return "result.dossier.xlsx";
    }

    @Override
    public OfficeFormat format() {
        return OfficeFormat.XLSX;
    }

    @Override
    public Class<ResultReportModel> modelClass() {
        return ResultReportModel.class;
    }

    @Override
    public String defaultFilename(ResultReportModel model) {
        return "CR-%s-%s.xlsx"
                .formatted(
                        model.labFile().labFileId(), FILE_DATE.format(model.labFile().openedAt()));
    }

    @Override
    public byte[] build(ResultReportModel model) {
        try (var wb = new XSSFWorkbook();
                var bos = new ByteArrayOutputStream()) {
            writeDossierSheet(wb, model);
            writeResultsSheet(wb, model);
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new OfficeRenderException(id(), "build_failed", ex);
        }
    }

    private static void writeDossierSheet(XSSFWorkbook wb, ResultReportModel model) {
        XSSFSheet sheet = wb.createSheet("Dossier");
        int rowNum = 0;

        rowNum = addLabelValue(sheet, rowNum, "Laboratoire", model.labIdentity().name());
        rowNum = addLabelValue(sheet, rowNum, "Adresse", model.labIdentity().address());
        rowNum =
                addLabelValue(
                        sheet, rowNum, "Accréditation", model.labIdentity().accreditationNumber());
        rowNum = addLabelValue(sheet, rowNum, "", "");
        rowNum = addLabelValue(sheet, rowNum, "N° Dossier", model.labFile().dailyId());
        rowNum = addLabelValue(sheet, rowNum, "ID Dossier", model.labFile().labFileId());
        rowNum = addLabelValue(sheet, rowNum, "", "");

        var p = model.patient();
        rowNum =
                addLabelValue(
                        sheet,
                        rowNum,
                        "Patient",
                        p.lastName() + " " + p.firstName() + " (" + p.sex() + ")");
        rowNum = addLabelValue(sheet, rowNum, "Date de naissance", p.dateOfBirth());
        rowNum = addLabelValue(sheet, rowNum, "Age", p.ageDisplay());
        rowNum = addLabelValue(sheet, rowNum, "", "");

        if (model.requestingPractitioner() != null) {
            rowNum =
                    addLabelValue(
                            sheet,
                            rowNum,
                            "Prescripteur",
                            model.requestingPractitioner().displayName());
            rowNum = addLabelValue(sheet, rowNum, "", "");
        }

        rowNum =
                addLabelValue(
                        sheet,
                        rowNum,
                        "Validation biologique",
                        model.biologicalValidatedAtDisplay());
        rowNum =
                addLabelValue(
                        sheet, rowNum, "Biologiste", model.biologicalValidatedBy().displayName());

        if (model.signedByDisplay() != null) {
            addLabelValue(sheet, rowNum, "Signé par", model.signedByDisplay());
        }
    }

    private static int addLabelValue(XSSFSheet sheet, int rowNum, String label, String value) {
        XSSFRow row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowNum + 1;
    }

    private static void writeResultsSheet(XSSFWorkbook wb, ResultReportModel model) {
        XSSFSheet sheet = wb.createSheet("Résultats");

        // Header row
        XSSFRow header = sheet.createRow(0);
        header.createCell(COL_PAILLASSE).setCellValue("Paillasse");
        header.createCell(COL_CODE).setCellValue("Code");
        header.createCell(COL_LIBELLE).setCellValue("Libellé");
        header.createCell(COL_RESULT).setCellValue("Résultat");
        header.createCell(COL_NORMES).setCellValue("Normes");
        header.createCell(COL_UNIT).setCellValue("Unité");
        header.createCell(COL_OUT_OF_RANGE).setCellValue("Hors-norme");

        int rowNum = 1;
        for (var group : model.groups()) {
            for (var analysisRow : group.rows()) {
                XSSFRow row = sheet.createRow(rowNum++);
                row.createCell(COL_PAILLASSE).setCellValue(group.paillasseName());
                row.createCell(COL_CODE).setCellValue(analysisRow.code());
                row.createCell(COL_LIBELLE).setCellValue(analysisRow.label());
                row.createCell(COL_RESULT).setCellValue(analysisRow.valueDisplay());
                row.createCell(COL_NORMES).setCellValue(analysisRow.referenceRange());
                row.createCell(COL_UNIT).setCellValue(analysisRow.unit());
                row.createCell(COL_OUT_OF_RANGE)
                        .setCellValue(analysisRow.outOfRange() ? "OUI" : "NON");
            }
        }
    }
}
