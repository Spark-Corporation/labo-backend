package org.sparkcorporation.lims.export.api;

/** The two OOXML office formats SP5 exports. Each carries its MIME type and filename extension. */
public enum OfficeFormat {
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String mimeType;
    private final String extension;

    OfficeFormat(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }
}
