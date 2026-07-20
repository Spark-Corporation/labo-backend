package org.sparkcorporation.lims.export.api;

/**
 * SPI for a renderable office document. One implementation per (document, format) pair.
 *
 * <p>Implementations are Spring beans. By convention (ArchUnit-enforced in this module) they live
 * under {@code <consumer-module>.infra.export}. Their {@link #id()} must be globally unique;
 * collisions fail application startup (see {@code OfficeDocumentTypeRegistry}).
 *
 * <p>Unlike {@code PdfDocumentType}, there is no template: POI documents are constructed
 * programmatically, so each type owns its own {@link #build(Object)} returning the OOXML bytes.
 *
 * @param <T> the model type rendered by this document
 */
public interface OfficeDocumentType<T> {

    /** Stable globally-unique id, kebab-cased, e.g. {@code "result.dossier"}. */
    String id();

    /** The output format — drives MIME type, extension, and encryption path. */
    OfficeFormat format();

    /** Compile-time-known model class. */
    Class<T> modelClass();

    /** Default suggested filename for {@code Content-Disposition}. */
    String defaultFilename(T model);

    /** Build the OOXML document bytes from the model (POI). Must not return {@code null}. */
    byte[] build(T model);
}
