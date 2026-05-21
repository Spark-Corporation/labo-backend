package org.sparkcorporation.lims.pdf.api;

/**
 * Standard page geometries used by {@link PdfDocumentType#pageSize()}. Width / height are mm so the
 * engine can hand them straight to {@code PdfRendererBuilder.usePageSize(..., "mm")}.
 */
public enum PageSize {
    A4_PORTRAIT(210f, 297f),
    A4_LANDSCAPE(297f, 210f),
    A5_PORTRAIT(148f, 210f);

    private final float widthMm;
    private final float heightMm;

    PageSize(float widthMm, float heightMm) {
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    public float widthMm() {
        return widthMm;
    }

    public float heightMm() {
        return heightMm;
    }
}
