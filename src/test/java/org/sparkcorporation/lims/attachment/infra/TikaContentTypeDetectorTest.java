package org.sparkcorporation.lims.attachment.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TikaContentTypeDetectorTest {

    private final TikaContentTypeDetector detector = new TikaContentTypeDetector();

    @Test
    void detectsPdfFromMagicBytes() {
        byte[] pdf = "%PDF-1.7 fake body".getBytes(StandardCharsets.US_ASCII);
        assertThat(detector.detect(pdf, "scan.pdf")).isEqualTo("application/pdf");
    }

    @Test
    void detectsPngFromMagicBytes() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        assertThat(detector.detect(png, "scan.png")).isEqualTo("image/png");
    }

    @Test
    void ignoresALyingFilename() {
        byte[] pdf = "%PDF-1.7 fake body".getBytes(StandardCharsets.US_ASCII);
        // client claims .png — magic bytes win
        assertThat(detector.detect(pdf, "scan.png")).isEqualTo("application/pdf");
    }

    @Test
    void unknownBytesFallBackToOctetStream() {
        byte[] junk = {0x00, 0x01, 0x02, 0x03};
        assertThat(detector.detect(junk, "blob.bin")).isEqualTo("application/octet-stream");
    }
}
