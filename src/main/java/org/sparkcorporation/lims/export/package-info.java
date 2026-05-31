/**
 * `export` module — shared OOXML (DOCX/XLSX) rendering and password-protection infrastructure.
 *
 * <p>Public surface: {@link org.sparkcorporation.lims.export.api}. Consumers (order, result, stats,
 * …) implement {@link org.sparkcorporation.lims.export.api.OfficeDocumentType} as a Spring bean
 * colocated under {@code <consumer-module>.infra.export}.
 *
 * <p>Spec: docs/superpowers/specs/2026-05-31-document-exports-design.md (SP5).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Export")
package org.sparkcorporation.lims.export;
