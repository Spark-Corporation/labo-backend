package org.sparkcorporation.lims.iam.infra;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM envelope cipher for per-user private keys. Master key from config (env-overridable).
 */
@Component
public class PrivateKeyCipher {

    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int AES_256_KEY_BYTES = 32;

    private final SecretKey masterKey;
    private final SecureRandom random = new SecureRandom();

    public PrivateKeyCipher(@Value("${lims.signing.key-encryption-key}") String base64Key) {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "lims.signing.key-encryption-key must be valid base64", e);
        }
        if (raw.length != AES_256_KEY_BYTES) {
            throw new IllegalStateException(
                    "lims.signing.key-encryption-key must decode to 32 bytes (AES-256), got "
                            + raw.length);
        }
        this.masterKey = new SecretKeySpec(raw, "AES");
    }

    public Encrypted encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(TAG_BITS, iv));
            return new Encrypted(c.doFinal(plaintext), iv);
        } catch (Exception e) {
            throw new IllegalStateException("private-key encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] ciphertext, byte[] iv) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(TAG_BITS, iv));
            return c.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("private-key decryption failed", e);
        }
    }

    public record Encrypted(byte[] ciphertext, byte[] iv) {}
}
