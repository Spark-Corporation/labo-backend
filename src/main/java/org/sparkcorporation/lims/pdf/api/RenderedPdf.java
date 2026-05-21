package org.sparkcorporation.lims.pdf.api;

import java.time.Instant;
import java.util.Objects;

/**
 * Output of a synchronous {@link PdfRenderingService#render render(...)} call.
 *
 * <p>The byte array is defensively copied on construction and again on every {@link #bytes()} read
 * so callers cannot observe each other's mutations.
 */
public record RenderedPdf(byte[] bytes, String suggestedFilename, Instant generatedAt) {

    public RenderedPdf {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(suggestedFilename, "suggestedFilename");
        Objects.requireNonNull(generatedAt, "generatedAt");
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
