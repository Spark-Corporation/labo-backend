package org.sparkcorporation.lims.iam.infra;

import java.util.List;
import java.util.Optional;

import dev.sparksam.securedx.core.signing.UserKeyPair;
import dev.sparksam.securedx.core.signing.UserKeyPairStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LimsUserKeyPairStore implements UserKeyPairStore {

    private final UserKeyPairRepository repo;
    private final PrivateKeyCipher cipher;

    @Override
    @Transactional
    public void store(UserKeyPair kp) {
        var enc = cipher.encrypt(kp.privateKey());
        var e = new UserKeyPairEntity();
        e.setKeyId(kp.keyId());
        e.setSubject(kp.subject());
        e.setPublicKey(kp.publicKey());
        e.setPrivateKeyEncrypted(enc.ciphertext());
        e.setPrivateKeyIv(enc.iv());
        e.setCreatedAt(kp.createdAt());
        e.setExpiresAt(kp.expiresAt());
        e.setActive(kp.active());
        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserKeyPair> getActive(String subject) {
        return repo.findFirstBySubjectAndActiveTrue(subject).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserKeyPair> getByKeyId(String keyId) {
        return repo.findById(keyId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserKeyPair> getHistory(String subject) {
        return repo.findBySubjectOrderByCreatedAtDesc(subject).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deactivateAll(String subject) {
        repo.deactivateAll(subject);
    }

    private UserKeyPair toDomain(UserKeyPairEntity e) {
        byte[] priv = cipher.decrypt(e.getPrivateKeyEncrypted(), e.getPrivateKeyIv());
        return new UserKeyPair(
                e.getKeyId(),
                e.getSubject(),
                e.getPublicKey(),
                priv,
                e.getCreatedAt(),
                e.getExpiresAt(),
                e.isActive());
    }
}
