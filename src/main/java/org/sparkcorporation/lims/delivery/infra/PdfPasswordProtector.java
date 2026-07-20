package org.sparkcorporation.lims.delivery.infra;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Component;

/**
 * Encrypts a rendered PDF with a password so the result dossier can only be opened by the patient
 * holding the out-of-band password (CDC §IX). Uses PDFBox 2.0.x (on the classpath via {@code
 * openhtmltopdf-pdfbox}); the same password is used as both owner and user password.
 */
@Component
public class PdfPasswordProtector {

    /** AES key length in bits for PDF standard encryption. */
    private static final int ENCRYPTION_KEY_LENGTH = 128;

    /** Returns the input PDF encrypted so it can only be opened with {@code password}. */
    public byte[] protect(byte[] pdfBytes, String password) {
        try (var doc = PDDocument.load(pdfBytes)) {
            var permissions = new AccessPermission();
            permissions.setCanModify(false);
            var policy = new StandardProtectionPolicy(password, password, permissions);
            policy.setEncryptionKeyLength(ENCRYPTION_KEY_LENGTH);
            doc.protect(policy);
            var out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to password-protect PDF", e);
        }
    }
}
