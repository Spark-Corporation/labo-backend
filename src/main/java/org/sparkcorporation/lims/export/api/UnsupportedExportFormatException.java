package org.sparkcorporation.lims.export.api;

/** Thrown when a request asks for an export {@code format} a controller does not support. 400. */
public class UnsupportedExportFormatException extends RuntimeException {

    private final String requestedFormat;

    public UnsupportedExportFormatException(String requestedFormat, String expected) {
        super("unsupported format '" + requestedFormat + "' (expected " + expected + ")");
        this.requestedFormat = requestedFormat;
    }

    public String requestedFormat() {
        return requestedFormat;
    }
}
