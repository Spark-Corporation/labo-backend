package org.sparkcorporation.lims.pdf.api;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when a template cannot be located or parsed. Reason tags: {@code "template_not_found"},
 * {@code "template_parse_error"}.
 */
public final class PdfTemplateException extends PdfRenderException {
    public PdfTemplateException(
            String documentTypeId, String reasonTag, @Nullable Throwable cause) {
        super(documentTypeId, reasonTag, cause);
    }
}
