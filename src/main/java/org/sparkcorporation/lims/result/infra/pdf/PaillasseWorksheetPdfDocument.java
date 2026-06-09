package org.sparkcorporation.lims.result.infra.pdf;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.pdf.api.PageSize;
import org.sparkcorporation.lims.pdf.api.PdfDocumentType;

/** CDC §VII per-paillasse worklist — cross-dossier bench worksheet, A4 landscape. */
@Component
public class PaillasseWorksheetPdfDocument implements PdfDocumentType<PaillasseWorksheetModel> {

    @Override
    public String id() {
        return "result.paillasse-worksheet";
    }

    @Override
    public String templatePath() {
        return "result/paillasse-worksheet";
    }

    @Override
    public Class<PaillasseWorksheetModel> modelClass() {
        return PaillasseWorksheetModel.class;
    }

    @Override
    public PageSize pageSize() {
        return PageSize.A4_LANDSCAPE;
    }

    @Override
    public String defaultFilename(PaillasseWorksheetModel model) {
        return "paillasse-%s-%s-%s.pdf"
                .formatted(model.disciplineCode(), model.fromIso(), model.toIso());
    }
}
