package org.sparkcorporation.lims.pdf.api;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when OpenHTMLtoPDF / PDFBox itself fails. Reason tags: {@code "io_error"}, {@code
 * "font_missing"}, {@code "executor_saturated"}, {@code "unknown"}.
 */
public final class PdfEngineException extends PdfRenderException {
    public PdfEngineException(String documentTypeId, String reasonTag, @Nullable Throwable cause) {
        super(documentTypeId, reasonTag, cause);
    }
}
