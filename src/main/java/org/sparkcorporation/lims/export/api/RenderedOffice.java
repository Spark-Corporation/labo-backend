package org.sparkcorporation.lims.export.api;

import java.time.Instant;
import java.util.Objects;

/** Output of a synchronous {@link OfficeRenderingService#render render(...)} call. */
public record RenderedOffice(
        byte[] bytes, String suggestedFilename, OfficeFormat format, Instant generatedAt) {

    public RenderedOffice {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(suggestedFilename, "suggestedFilename");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(generatedAt, "generatedAt");
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
