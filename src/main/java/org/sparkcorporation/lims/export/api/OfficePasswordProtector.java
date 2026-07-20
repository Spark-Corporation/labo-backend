package org.sparkcorporation.lims.export.api;

/**
 * Password-protects an OOXML (.docx/.xlsx) byte stream using POI agile (AES) encryption. The same
 * call path serves both formats.
 */
public interface OfficePasswordProtector {

    /** Returns an encrypted OLE2 package that opens only with {@code password}. */
    byte[] protect(byte[] ooxml, String password);
}
