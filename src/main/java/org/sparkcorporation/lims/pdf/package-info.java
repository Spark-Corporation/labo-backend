/**
 * `pdf` module — shared HTML → PDF rendering infrastructure.
 *
 * <p>Public surface: {@link org.sparkcorporation.lims.pdf.api}. Consumers (result, billing,
 * cashbox, quote, …) implement {@link org.sparkcorporation.lims.pdf.api.PdfDocumentType} as a
 * Spring bean colocated under {@code <consumer-module>.infra.pdf}.
 *
 * <p>Spec: docs/superpowers/specs/2026-05-20-pdf-renderer-design.md (1.5.c).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Pdf")
package org.sparkcorporation.lims.pdf;
