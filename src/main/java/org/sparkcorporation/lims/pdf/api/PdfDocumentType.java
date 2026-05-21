package org.sparkcorporation.lims.pdf.api;

/**
 * SPI for a renderable document. One implementation per (template, model) pair.
 *
 * <p>Implementations are Spring beans. By convention (ArchUnit-enforced in chunk 7) they live under
 * {@code <consumer-module>.infra.pdf}. Their {@link #id()} must be globally unique; collisions fail
 * application startup (see {@code PdfDocumentTypeRegistry} in chunk 3).
 *
 * @param <T> the model type rendered by this document
 */
public interface PdfDocumentType<T> {

    /**
     * Stable globally-unique identifier. Used in metrics tags, log MDC, the {@code
     * pdf_render_jobs.document_type_id} column. Convention: {@code <consumer>.<document>},
     * kebab-cased, e.g. {@code "result.compte-rendu"}.
     */
    String id();

    /**
     * Classpath resource of the Thymeleaf template, e.g. {@code "result/cr"}. Resolved by the
     * engine's {@code SpringTemplateEngine} rooted at {@code classpath:/templates/}; the {@code
     * .html} suffix is implicit.
     */
    String templatePath();

    /** Compile-time-known model class. Used by Jackson on async deserialization. */
    Class<T> modelClass();

    /**
     * Default suggested filename for {@code Content-Disposition}. Implementations may reference
     * fields of the model (lab-file id, dates, …).
     */
    String defaultFilename(T model);

    /** Page geometry. Defaults to A4 portrait. */
    default PageSize pageSize() {
        return PageSize.A4_PORTRAIT;
    }
}
