package org.sparkcorporation.lims.export.infra;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.export.api.ExportPasswordGenerator;

/**
 * {@link ExportPasswordGenerator} backed by {@link SecureRandom}. Crockford-style base32 alphabet
 * (no I/L/O/U), mirroring SP4's dossier-send password idiom.
 */
@Component
public class SecureRandomExportPasswordGenerator implements ExportPasswordGenerator {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        var sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
