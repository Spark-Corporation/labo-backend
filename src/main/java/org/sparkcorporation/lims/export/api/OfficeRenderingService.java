package org.sparkcorporation.lims.export.api;

/** Public engine API for office-document rendering. Implementation in {@code export.infra}. */
public interface OfficeRenderingService {

    /**
     * Synchronous render. Delegates to {@link OfficeDocumentType#build}, stamps {@code
     * generatedAt}, and wraps any failure in {@link OfficeRenderException}.
     */
    <T> RenderedOffice render(OfficeDocumentType<T> docType, T model);
}
