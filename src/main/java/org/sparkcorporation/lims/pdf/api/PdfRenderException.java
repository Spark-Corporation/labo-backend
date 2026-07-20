package org.sparkcorporation.lims.pdf.api;

import org.jspecify.annotations.Nullable;

/**
 * Base of the PDF-render exception hierarchy. Subclasses pin a concrete {@link #reasonTag()} that
 * matches the metrics tag in {@code lims.pdf.render.failures{reason=...}}.
 *
 * <p>The message is safe to log — it contains only the document type id and the reason tag, never
 * model content or rendered bytes. Subclasses preserve the cause for diagnostic logging at the
 * integration boundary.
 */
public abstract class PdfRenderException extends RuntimeException {

    private final String documentTypeId;
    private final String reasonTag;

    protected PdfRenderException(
            String documentTypeId, String reasonTag, @Nullable Throwable cause) {
        super("pdf-render failed: documentType=" + documentTypeId + " reason=" + reasonTag, cause);
        this.documentTypeId = documentTypeId;
        this.reasonTag = reasonTag;
    }

    public String documentTypeId() {
        return documentTypeId;
    }

    public String reasonTag() {
        return reasonTag;
    }
}
