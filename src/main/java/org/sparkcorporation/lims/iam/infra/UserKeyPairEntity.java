package org.sparkcorporation.lims.iam.infra;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * JPA mirror of secured-x's {@code UserKeyPair}. The raw PKCS#8 private key is never stored as-is:
 * {@link LimsUserKeyPairStore} encrypts it with {@link PrivateKeyCipher} (AES-256-GCM) into {@code
 * privateKeyEncrypted} + {@code privateKeyIv} before persisting. No optimistic-lock {@code version}
 * — the library treats key pairs as append/deactivate, not concurrent-edit.
 */
@Entity
@Table(name = "user_key_pairs")
@Getter
@Setter
@NoArgsConstructor
public class UserKeyPairEntity {

    @Id
    @Column(name = "key_id", length = 36, nullable = false)
    private String keyId;

    @Column(name = "subject", length = 26, nullable = false)
    private String subject;

    @Column(name = "public_key", nullable = false)
    private byte[] publicKey;

    @Column(name = "private_key_encrypted", nullable = false)
    private byte[] privateKeyEncrypted;

    @Column(name = "private_key_iv", nullable = false)
    private byte[] privateKeyIv;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private @Nullable Instant expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active;
}
