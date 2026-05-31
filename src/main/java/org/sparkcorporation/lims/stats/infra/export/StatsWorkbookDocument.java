package org.sparkcorporation.lims.stats.infra.export;

import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.export.api.OfficeDocumentType;
import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.export.api.OfficeRenderException;
import org.sparkcorporation.lims.stats.infra.ActivityRow;
import org.sparkcorporation.lims.stats.infra.AnalysisStatRow;
import org.sparkcorporation.lims.stats.infra.AntibiogramStatRow;
import org.sparkcorporation.lims.stats.infra.DisciplineStatRow;
import org.sparkcorporation.lims.stats.infra.PatientStatsRow;
import org.sparkcorporation.lims.stats.infra.PrescriberStatRow;

/**
 * {@link OfficeDocumentType} for the statistics XLSX workbook (CDC §Onglet Statistiques / SP5). One
 * sheet per SP1 dataset; columns are unfiltered — date range only (YAGNI per spec §3/§10).
 */
@Component
public class StatsWorkbookDocument implements OfficeDocumentType<StatsWorkbookModel> {

    // Activity sheet column indices (Dossiers | Urgents | Validés | Patients total | Nouveaux)
    private static final int COL_ACT_PATIENTS_NEW = 4;

    // Analyses sheet column indices (Code | Analyse | Nb résultés | Nb normaux | Nb élevés | Nb
    // bas)
    private static final int COL_ANA_HIGH = 4;
    private static final int COL_ANA_LOW = 5;

    // Antibiograms sheet column indices (Code | Antibiotique | S | I | R)
    private static final int COL_ABX_RESISTANT = 4;

    // Patients sheet: first data row of the sex breakdown section (after cohort block + blank row
    // + sex header)
    private static final int PATIENTS_SEX_DATA_START_ROW = 4;

    @Override
    public String id() {
        return "stats.workbook";
    }

    @Override
    public OfficeFormat format() {
        return OfficeFormat.XLSX;
    }

    @Override
    public Class<StatsWorkbookModel> modelClass() {
        return StatsWorkbookModel.class;
    }

    @Override
    public String defaultFilename(StatsWorkbookModel model) {
        return "STATISTIQUES-%s-%s.xlsx".formatted(model.from(), model.to());
    }

    @Override
    public byte[] build(StatsWorkbookModel model) {
        try (var wb = new XSSFWorkbook();
                var bos = new ByteArrayOutputStream()) {
            writeActivitySheet(wb, model.activity());
            writeDisciplineSheet(wb, model);
            writePrescriberSheet(wb, model);
            writeAnalysesSheet(wb, model);
            writeAntibiogramsSheet(wb, model);
            writePatientsSheet(wb, model.patients());
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new OfficeRenderException(id(), "build_failed", ex);
        }
    }

    // ---- Activity -------------------------------------------------------------------------------

    private static void writeActivitySheet(XSSFWorkbook wb, ActivityRow activity) {
        XSSFSheet sheet = wb.createSheet("Activité");
        Row header = sheet.createRow(0);
        setString(header, 0, "Dossiers");
        setString(header, 1, "Urgents");
        setString(header, 2, "Validés");
        setString(header, 3, "Patients (total)");
        setString(header, COL_ACT_PATIENTS_NEW, "Nouveaux patients");
        Row data = sheet.createRow(1);
        setLong(data, 0, activity.dossiers());
        setLong(data, 1, activity.urgents());
        setLong(data, 2, activity.validated());
        setLong(data, 3, activity.patientsAll());
        setLong(data, COL_ACT_PATIENTS_NEW, activity.patientsNew());
    }

    // ---- By discipline --------------------------------------------------------------------------

    private static void writeDisciplineSheet(XSSFWorkbook wb, StatsWorkbookModel model) {
        XSSFSheet sheet = wb.createSheet("Par discipline");
        Row header = sheet.createRow(0);
        setString(header, 0, "Code");
        setString(header, 1, "Discipline");
        setString(header, 2, "Nb résultats");
        setString(header, 3, "Nb anormaux");
        int rowIdx = 1;
        for (DisciplineStatRow r : model.byDiscipline()) {
            Row row = sheet.createRow(rowIdx++);
            setString(row, 0, r.code());
            setString(row, 1, r.name());
            setLong(row, 2, r.resultCount());
            setLong(row, 3, r.abnormalCount());
        }
    }

    // ---- By prescriber --------------------------------------------------------------------------

    private static void writePrescriberSheet(XSSFWorkbook wb, StatsWorkbookModel model) {
        XSSFSheet sheet = wb.createSheet("Par prescripteur");
        Row header = sheet.createRow(0);
        setString(header, 0, "Code");
        setString(header, 1, "Prescripteur");
        setString(header, 2, "Nb dossiers");
        int rowIdx = 1;
        for (PrescriberStatRow r : model.byPrescriber()) {
            Row row = sheet.createRow(rowIdx++);
            setString(row, 0, r.code());
            setString(row, 1, r.name());
            setLong(row, 2, r.dossierCount());
        }
    }

    // ---- Analyses -------------------------------------------------------------------------------

    private static void writeAnalysesSheet(XSSFWorkbook wb, StatsWorkbookModel model) {
        XSSFSheet sheet = wb.createSheet("Analyses");
        Row header = sheet.createRow(0);
        setString(header, 0, "Code");
        setString(header, 1, "Analyse");
        setString(header, 2, "Nb résultés");
        setString(header, 3, "Nb normaux");
        setString(header, COL_ANA_HIGH, "Nb élevés");
        setString(header, COL_ANA_LOW, "Nb bas");
        int rowIdx = 1;
        for (AnalysisStatRow r : model.analyses()) {
            Row row = sheet.createRow(rowIdx++);
            setString(row, 0, r.code());
            setString(row, 1, r.name());
            setLong(row, 2, r.resultedCount());
            setLong(row, 3, r.normalCount());
            setLong(row, COL_ANA_HIGH, r.highCount());
            setLong(row, COL_ANA_LOW, r.lowCount());
        }
    }

    // ---- Antibiograms ---------------------------------------------------------------------------

    private static void writeAntibiogramsSheet(XSSFWorkbook wb, StatsWorkbookModel model) {
        XSSFSheet sheet = wb.createSheet("Antibiogrammes");
        Row header = sheet.createRow(0);
        setString(header, 0, "Code");
        setString(header, 1, "Antibiotique");
        setString(header, 2, "S (Sensible)");
        setString(header, 3, "I (Intermédiaire)");
        setString(header, COL_ABX_RESISTANT, "R (Résistant)");
        int rowIdx = 1;
        for (AntibiogramStatRow r : model.antibiograms()) {
            Row row = sheet.createRow(rowIdx++);
            setString(row, 0, r.code());
            setString(row, 1, r.name());
            setLong(row, 2, r.sensible());
            setLong(row, 3, r.intermediate());
            setLong(row, COL_ABX_RESISTANT, r.resistant());
        }
    }

    // ---- Patients -------------------------------------------------------------------------------

    private static void writePatientsSheet(XSSFWorkbook wb, PatientStatsRow patients) {
        XSSFSheet sheet = wb.createSheet("Patients");
        // Cohort summary block
        Row cohortHeader = sheet.createRow(0);
        setString(cohortHeader, 0, "Total patients");
        setString(cohortHeader, 1, "Nouveaux");
        Row cohortData = sheet.createRow(1);
        setLong(cohortData, 0, patients.allCount());
        setLong(cohortData, 1, patients.newCount());

        // Sex breakdown block (blank separator row)
        sheet.createRow(2);
        Row sexHeader = sheet.createRow(3);
        setString(sexHeader, 0, "Sexe");
        setString(sexHeader, 1, "Nb patients");
        int rowIdx = PATIENTS_SEX_DATA_START_ROW;
        for (var entry : patients.bySex().entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            setString(row, 0, entry.getKey());
            setLong(row, 1, entry.getValue());
        }

        // Age-band breakdown block (blank separator row)
        rowIdx++;
        Row bandHeader = sheet.createRow(rowIdx++);
        setString(bandHeader, 0, "Catégorie");
        setString(bandHeader, 1, "Nb patients");
        for (var entry : patients.byAgeBand().entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            setString(row, 0, entry.getKey());
            setLong(row, 1, entry.getValue());
        }
    }

    // ---- Cell helpers ---------------------------------------------------------------------------

    private static void setString(Row row, int col, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
    }

    private static void setLong(Row row, int col, long value) {
        Cell cell = row.createCell(col);
        cell.setCellValue((double) value);
    }
}
