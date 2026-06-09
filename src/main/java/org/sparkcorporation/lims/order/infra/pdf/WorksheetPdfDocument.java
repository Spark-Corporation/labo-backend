package org.sparkcorporation.lims.order.infra.pdf;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.pdf.api.PageSize;
import org.sparkcorporation.lims.pdf.api.PdfDocumentType;

/** CDC §II « feuille de paillasse » — per-dossier bench worksheet, A4 portrait. */
@Component
public class WorksheetPdfDocument implements PdfDocumentType<WorksheetModel> {

    @Override
    public String id() {
        return "order.worksheet";
    }

    @Override
    public String templatePath() {
        return "order/worksheet";
    }

    @Override
    public Class<WorksheetModel> modelClass() {
        return WorksheetModel.class;
    }

    @Override
    public PageSize pageSize() {
        return PageSize.A4_PORTRAIT;
    }

    @Override
    public String defaultFilename(WorksheetModel model) {
        return "feuille-paillasse-%s.pdf".formatted(model.header().registrationNumber());
    }
}
