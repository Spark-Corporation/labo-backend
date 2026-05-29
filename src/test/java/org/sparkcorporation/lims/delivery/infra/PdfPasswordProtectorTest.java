package org.sparkcorporation.lims.delivery.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.jupiter.api.Test;

class PdfPasswordProtectorTest {

    private static final String PASSWORD = "s3cret-pw";

    private final PdfPasswordProtector protector = new PdfPasswordProtector();

    private static byte[] blankPdf() throws IOException {
        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage());
            var out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void protect_produces_non_empty_encrypted_pdf() throws IOException {
        byte[] protectedPdf = protector.protect(blankPdf(), PASSWORD);

        assertThat(protectedPdf).isNotEmpty();
        try (var doc = PDDocument.load(protectedPdf, PASSWORD)) {
            assertThat(doc.isEncrypted()).isTrue();
        }
    }

    @Test
    void protect_output_cannot_be_opened_without_password() throws IOException {
        byte[] protectedPdf = protector.protect(blankPdf(), PASSWORD);

        assertThatThrownBy(
                        () -> {
                            try (var ignored = PDDocument.load(protectedPdf)) {
                                // load must throw before reaching here
                            }
                        })
                .isInstanceOf(InvalidPasswordException.class);
    }
}
