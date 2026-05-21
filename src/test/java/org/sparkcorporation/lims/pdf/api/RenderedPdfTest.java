package org.sparkcorporation.lims.pdf.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class RenderedPdfTest {

    private static final byte[] BYTES = new byte[] {'P', 'D', 'F'};
    private static final String NAME = "doc.pdf";
    private static final Instant AT = Instant.parse("2026-05-20T10:00:00Z");

    @Test
    void defensiveCopyOnConstructionAndOnRead() {
        var input = BYTES.clone();
        var rendered = new RenderedPdf(input, NAME, AT);

        input[0] = 'X';
        assertThat(rendered.bytes()[0]).isEqualTo((byte) 'P');

        var read = rendered.bytes();
        read[0] = 'Y';
        assertThat(rendered.bytes()[0]).isEqualTo((byte) 'P');
    }

    @Test
    void rejectsNullFields() {
        assertThatNullPointerException().isThrownBy(() -> new RenderedPdf(null, NAME, AT));
        assertThatNullPointerException().isThrownBy(() -> new RenderedPdf(BYTES, null, AT));
        assertThatNullPointerException().isThrownBy(() -> new RenderedPdf(BYTES, NAME, null));
    }

    @Test
    void exposesSuggestedFilenameAndGeneratedAt() {
        var rendered = new RenderedPdf(BYTES, NAME, AT);
        assertThat(rendered.suggestedFilename()).isEqualTo(NAME);
        assertThat(rendered.generatedAt()).isEqualTo(AT);
    }
}
