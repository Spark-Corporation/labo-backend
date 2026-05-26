package org.sparkcorporation.lims.pdf.infra;

import org.sparkcorporation.lims.pdf.api.PdfRenderJobRecord;

/**
 * Maps the JPA entity {@link PdfRenderJob} to the public {@link PdfRenderJobRecord} carrier.
 *
 * <p>Lives under {@code pdf.infra} (not {@code pdf.web}) to avoid an {@code infra → web} import
 * direction that Spring Modulith would flag. The public visibility lets {@code PdfJobController}
 * (under {@code pdf.web}) reuse the mapper.
 */
public final class PdfJobResponseMapper {

    private PdfJobResponseMapper() {}

    public static PdfRenderJobRecord toRecord(PdfRenderJob entity) {
        return new PdfRenderJobRecord(
                entity.getId(),
                entity.getDocumentTypeId(),
                entity.getStatus(),
                entity.getBytes(),
                entity.getSuggestedFilename(),
                entity.getErrorMessage(),
                entity.getRequestedAt(),
                entity.getCompletedAt(),
                entity.getRequestedByUserId());
    }
}
