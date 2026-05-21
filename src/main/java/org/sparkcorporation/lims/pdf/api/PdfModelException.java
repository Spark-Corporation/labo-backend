package org.sparkcorporation.lims.pdf.api;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when the model cannot be deserialized (async path) or violates Thymeleaf evaluation.
 * Reason tag: {@code "model_invalid"}.
 */
public final class PdfModelException extends PdfRenderException {
    public PdfModelException(String documentTypeId, String reasonTag, @Nullable Throwable cause) {
        super(documentTypeId, reasonTag, cause);
    }
}
