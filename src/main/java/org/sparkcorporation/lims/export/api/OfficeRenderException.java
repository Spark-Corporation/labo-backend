package org.sparkcorporation.lims.export.api;

/** Thrown by the office engine on any build/encrypt failure. Mapped to 500 by the web advice. */
public class OfficeRenderException extends RuntimeException {

    private final String documentTypeId;
    private final String reasonTag;

    public OfficeRenderException(String documentTypeId, String reasonTag, Throwable cause) {
        super("office render failed for '" + documentTypeId + "' (" + reasonTag + ")", cause);
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
