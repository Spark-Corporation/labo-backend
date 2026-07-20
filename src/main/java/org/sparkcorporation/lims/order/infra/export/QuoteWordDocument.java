package org.sparkcorporation.lims.order.infra.export;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.export.api.OfficeDocumentType;
import org.sparkcorporation.lims.export.api.OfficeFormat;
import org.sparkcorporation.lims.export.api.OfficeRenderException;
import org.sparkcorporation.lims.order.infra.pdf.QuoteReportModel;

/** {@link OfficeDocumentType} for the quote (devis) Word export (CDC §III). */
@Component
public class QuoteWordDocument implements OfficeDocumentType<QuoteReportModel> {

    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    private static final int FONT_PT_TITLE = 14;

    @Override
    public String id() {
        return "order.quote";
    }

    @Override
    public OfficeFormat format() {
        return OfficeFormat.DOCX;
    }

    @Override
    public Class<QuoteReportModel> modelClass() {
        return QuoteReportModel.class;
    }

    @Override
    public String defaultFilename(QuoteReportModel model) {
        return "DEVIS-%s-%s.docx"
                .formatted(
                        model.header().quoteNumber(),
                        FILE_DATE.format(model.header().createdAtUtc()));
    }

    @Override
    public byte[] build(QuoteReportModel model) {
        try (var doc = new XWPFDocument();
                var bos = new ByteArrayOutputStream()) {
            var lab = model.labIdentity();
            heading(doc, lab.name());
            paragraph(doc, lab.address() + "  —  " + lab.accreditationNumber());
            paragraph(
                    doc,
                    model.statusBanner().title()
                            + (model.statusBanner().subtitle() == null
                                    ? ""
                                    : " (" + model.statusBanner().subtitle() + ")"));
            paragraph(
                    doc,
                    "Devis N° "
                            + model.header().quoteNumber()
                            + "   du "
                            + model.header().createdAtDisplay());

            var p = model.patient();
            paragraph(
                    doc,
                    "Patient : "
                            + p.lastName()
                            + " "
                            + p.firstName()
                            + "  ("
                            + p.sex()
                            + ")   Code : "
                            + p.labCode());

            XWPFTable table = doc.createTable();
            headerRow(table, "Code", "Libellé", "Indice B", "Qté", "Total");
            for (var line : model.lines()) {
                bodyRow(
                        table,
                        line.code(),
                        line.label(),
                        line.bRateDisplay(),
                        Integer.toString(line.labelCount()),
                        line.lineTotalDisplay());
            }
            paragraph(doc, "Montant total : " + model.totals().totalAmountDisplay());

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

    private static void paragraph(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        XWPFRun run = para.createRun();
        run.setText(text);
    }

    private static void headerRow(XWPFTable table, String... headers) {
        XWPFTableRow row = table.getRow(0);
        // The first cell already exists; set its text
        row.getCell(0).setText(headers[0]);
        boldCell(row.getCell(0));
        for (int i = 1; i < headers.length; i++) {
            var cell = row.addNewTableCell();
            cell.setText(headers[i]);
            boldCell(cell);
        }
    }

    private static void boldCell(org.apache.poi.xwpf.usermodel.XWPFTableCell cell) {
        for (var para : cell.getParagraphs()) {
            for (var run : para.getRuns()) {
                run.setBold(true);
            }
        }
    }

    private static void bodyRow(XWPFTable table, String... values) {
        XWPFTableRow row = table.createRow();
        // createRow creates cells matching the header column count; set each
        for (int i = 0; i < values.length && i < row.getTableCells().size(); i++) {
            row.getCell(i).setText(values[i]);
        }
        // If the row has fewer cells than values (shouldn't happen but be safe), add them
        for (int i = row.getTableCells().size(); i < values.length; i++) {
            row.addNewTableCell().setText(values[i]);
        }
    }
}
