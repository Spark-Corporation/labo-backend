package org.sparkcorporation.lims.iam.infra;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import dev.sparksam.securedx.core.signing.UserKeyPair;
import dev.sparksam.securedx.core.signing.UserKeyPairManager;
import dev.sparksam.securedx.core.signing.UserKeyPairStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The reserved {@code SYSTEM_AUDIT} signing identity used to sign audit checkpoints. Its key pair
 * is provisioned lazily and persisted KEK-encrypted in {@code user_key_pairs} via the existing
 * {@link UserKeyPairStore}; the FK target {@code users} row is seed changeset 202606101001.
 *
 * <p>secured-x's {@code UserKeyPairManager} generates Ed25519 keys via JCA {@code
 * KeyPairGenerator("Ed25519")} and persists each side's {@code getEncoded()} bytes — i.e. X.509
 * SubjectPublicKeyInfo for the public key and PKCS#8 PrivateKeyInfo for the private key. So the JCA
 * {@code KeyFactory("Ed25519")} round-trip below works directly; no BouncyCastle is required.
 */
@Component
@RequiredArgsConstructor
public class SystemAuditIdentity {

    /**
     * Fixed, documented, Crockford-valid (no I/L/O/U) 26-char ULID. Matches seed user 202606101001.
     */
    public static final String SUBJECT = "01KSYS0AD1T000000000000000";

    private final UserKeyPairStore keyPairStore;
    private final UserKeyPairManager keyPairManager;

    public String ensureProvisioned() {
        return keyPairStore
                .getActive(SUBJECT)
                .map(UserKeyPair::keyId)
                .orElseGet(() -> keyPairManager.generateKeyPair(SUBJECT).keyId());
    }

    public String keyId() {
        return ensureProvisioned();
    }

    public String sign(byte[] payload) {
        ensureProvisioned();
        UserKeyPair kp = keyPairStore.getActive(SUBJECT).orElseThrow();
        try {
            PrivateKey priv =
                    KeyFactory.getInstance("Ed25519")
                            .generatePrivate(new PKCS8EncodedKeySpec(kp.privateKey()));
            Signature s = Signature.getInstance("Ed25519");
            s.initSign(priv);
            s.update(payload);
            return Base64.getEncoder().encodeToString(s.sign());
        } catch (Exception e) {
            throw new IllegalStateException("checkpoint signing failed", e);
        }
    }

    public boolean verify(byte[] payload, String signatureB64) {
        UserKeyPair kp = keyPairStore.getActive(SUBJECT).orElseThrow();
        try {
            PublicKey pub =
                    KeyFactory.getInstance("Ed25519")
                            .generatePublic(new X509EncodedKeySpec(kp.publicKey()));
            Signature s = Signature.getInstance("Ed25519");
            s.initVerify(pub);
            s.update(payload);
            return s.verify(Base64.getDecoder().decode(signatureB64));
        } catch (Exception e) {
            return false;
        }
    }
}
