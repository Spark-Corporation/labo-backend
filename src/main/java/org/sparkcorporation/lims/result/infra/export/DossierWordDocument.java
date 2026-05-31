package org.sparkcorporation.lims.result.infra.export;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.export.api.OfficeDocumentType;
import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.export.api.OfficeRenderException;
import org.sparkcorporation.lims.result.infra.pdf.ResultReportModel;

/**
 * {@link OfficeDocumentType} for the result dossier (compte-rendu) Word export (CDC §IV).
 * Password-protected; the controller generates and returns the password in {@code
 * X-Export-Password}.
 */
@Component
public class DossierWordDocument implements OfficeDocumentType<ResultReportModel> {

    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    private static final int FONT_PT_TITLE = 14;
    private static final int FONT_PT_HEADING = 12;

    @Override
    public String id() {
        return "result.dossier";
    }

    @Override
    public OfficeFormat format() {
        return OfficeFormat.DOCX;
    }

    @Override
    public Class<ResultReportModel> modelClass() {
        return ResultReportModel.class;
    }

    @Override
    public String defaultFilename(ResultReportModel model) {
        return "CR-%s-%s.docx"
                .formatted(
                        model.labFile().labFileId(), FILE_DATE.format(model.labFile().openedAt()));
    }

    @Override
    public byte[] build(ResultReportModel model) {
        try (var doc = new XWPFDocument();
                var bos = new ByteArrayOutputStream()) {
            var lab = model.labIdentity();
            heading(doc, lab.name());
            paragraph(doc, lab.address() + "  —  " + lab.accreditationNumber());

            paragraph(
                    doc,
                    "Dossier N° "
                            + model.labFile().dailyId()
                            + "  (ID: "
                            + model.labFile().labFileId()
                            + ")");

            var p = model.patient();
            paragraph(
                    doc,
                    "Patient : "
                            + p.lastName()
                            + " "
                            + p.firstName()
                            + "  ("
                            + p.sex()
                            + ")   Né(e) le : "
                            + p.dateOfBirth()
                            + "   Age : "
                            + p.ageDisplay());

            if (model.requestingPractitioner() != null) {
                paragraph(doc, "Prescripteur : " + model.requestingPractitioner().displayName());
            }

            paragraph(doc, "Validation biologique : " + model.biologicalValidatedAtDisplay());
            paragraph(doc, "Biologiste : " + model.biologicalValidatedBy().displayName());

            if (model.signedByDisplay() != null) {
                paragraph(
                        doc,
                        "Signé par : "
                                + model.signedByDisplay()
                                + (model.signedAtDisplay() != null
                                        ? " le " + model.signedAtDisplay()
                                        : ""));
            }

            for (var group : model.groups()) {
                subheading(doc, group.paillasseName());
                XWPFTable table = doc.createTable();
                headerRow(table, "Code", "Libellé", "Résultat", "Normes", "Unité");
                for (var row : group.rows()) {
                    analysisBodyRow(table, row);
                }
            }

            if (!model.antibiograms().isEmpty()) {
                subheading(doc, "Antibiogrammes");
                for (var abx : model.antibiograms()) {
                    paragraph(doc, "Germe : " + abx.sampleLabel());
                    XWPFTable abxTable = doc.createTable();
                    headerRow(abxTable, "Antibiotique", "Diamètre (mm)", "Interprétation");
                    for (var reading : abx.readings()) {
                        bodyRow(
                                abxTable,
                                reading.antibioticSigle(),
                                reading.diameterMm(),
                                reading.interpretation());
                    }
                }
            }

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new OfficeRenderException(id(), "build_failed", ex);
        }
    }

    private static void heading(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(FONT_PT_TITLE);
    }

    private static void subheading(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(FONT_PT_HEADING);
    }

    private static void paragraph(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        XWPFRun run = para.createRun();
        run.setText(text);
    }

    private static void headerRow(XWPFTable table, String... headers) {
        XWPFTableRow row = table.getRow(0);
        row.getCell(0).setText(headers[0]);
        boldCell(row.getCell(0));
        for (int i = 1; i < headers.length; i++) {
            var cell = row.addNewTableCell();
            cell.setText(headers[i]);
            boldCell(cell);
        }
    }

    private static void boldCell(XWPFTableCell cell) {
        for (var para : cell.getParagraphs()) {
            for (var run : para.getRuns()) {
                run.setBold(true);
            }
        }
    }

    private static void analysisBodyRow(XWPFTable table, ResultReportModel.AnalysisRow row) {
        XWPFTableRow tableRow = table.createRow();
        String[] values = {
            row.code(), row.label(), row.valueDisplay(), row.referenceRange(), row.unit()
        };
        for (int i = 0; i < values.length && i < tableRow.getTableCells().size(); i++) {
            tableRow.getCell(i).setText(values[i]);
        }
        for (int i = tableRow.getTableCells().size(); i < values.length; i++) {
            tableRow.addNewTableCell().setText(values[i]);
        }
        // Bold the result value cell when out-of-range
        if (row.outOfRange()) {
            boldValueCell(tableRow, 2);
        }
        // Render comment as a continuation row under the analysis row
        if (row.comment() != null) {
            XWPFTableRow commentRow = table.createRow();
            commentRow.getCell(0).setText("↳ " + row.comment());
        }
    }

    private static void boldValueCell(XWPFTableRow row, int cellIndex) {
        if (cellIndex < row.getTableCells().size()) {
            boldCell(row.getCell(cellIndex));
        }
    }

    private static void bodyRow(XWPFTable table, String... values) {
        XWPFTableRow row = table.createRow();
        for (int i = 0; i < values.length && i < row.getTableCells().size(); i++) {
            row.getCell(i).setText(values[i]);
        }
        for (int i = row.getTableCells().size(); i < values.length; i++) {
            row.addNewTableCell().setText(values[i]);
        }
    }
}
