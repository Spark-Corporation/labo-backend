package org.sparkcorporation.lims.pdf.infra;

import org.sparkcorporation.lims.pdf.api.PdfRenderJobView;

/**
 * Maps the JPA entity {@link PdfRenderJob} to the public {@link PdfRenderJobView} record.
 *
 * <p>Lives under {@code pdf.infra} (not {@code pdf.web}) to avoid an {@code infra → web} import
 * direction that Spring Modulith would flag. The public visibility lets {@code PdfJobController}
 * (under {@code pdf.web}) reuse the mapper.
 */
public final class PdfJobResponseMapper {

    private PdfJobResponseMapper() {}

    public static PdfRenderJobView toView(PdfRenderJob row) {
        return new PdfRenderJobView(
                row.getId(),
                row.getDocumentTypeId(),
                row.getStatus(),
                row.getBytes(),
                row.getSuggestedFilename(),
                row.getErrorMessage(),
                row.getRequestedAt(),
                row.getCompletedAt());
    }
}
