package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class PrivateKeyCipherTest {

    // 32 bytes, base64
    private static final String KEY =
            Base64.getEncoder()
                    .encodeToString(
                            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void encrypt_then_decrypt_roundtrips() {
        var cipher = new PrivateKeyCipher(KEY);
        byte[] plain = "a-fake-pkcs8-private-key".getBytes(StandardCharsets.UTF_8);
        var enc = cipher.encrypt(plain);
        assertThat(enc.iv()).hasSize(12);
        assertThat(enc.ciphertext()).isNotEqualTo(plain);
        assertThat(cipher.decrypt(enc.ciphertext(), enc.iv())).isEqualTo(plain);
    }

    @Test
    void distinct_iv_per_encryption() {
        var cipher = new PrivateKeyCipher(KEY);
        byte[] plain = "x".getBytes(StandardCharsets.UTF_8);
        assertThat(cipher.encrypt(plain).iv()).isNotEqualTo(cipher.encrypt(plain).iv());
    }

    @Test
    void rejects_wrong_size_master_key() {
        assertThatThrownBy(
                        () ->
                                new PrivateKeyCipher(
                                        Base64.getEncoder().encodeToString("short".getBytes())))
                .isInstanceOf(IllegalStateException.class);
    }
}
