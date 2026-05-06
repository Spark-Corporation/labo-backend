package org.sparkcorporation.lims.iam.infra;

import java.time.Instant;
import java.util.Optional;

import dev.sparksam.securedx.provider.local.LocalCredentialStore;
import dev.sparksam.securedx.provider.local.StoredCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LimsLocalCredentialStore implements LocalCredentialStore {

    private final UserRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredCredential> findByUsername(String username) {
        // Note: User entity's @SQLRestriction("deleted_at IS NULL") already filters soft-deleted
        // users at the JPA layer — findByEmailIgnoreCase only returns active rows.
        return repo.findByEmailIgnoreCase(username)
                .map(
                        u ->
                                new StoredCredential(
                                        u.id().value(), u.email(), u.passwordHash(), !u.enabled()));
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(String subject, Instant when) {
        repo.findById(subject).ifPresent(u -> u.recordLogin(when));
    }
}
