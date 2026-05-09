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

    private static final String ARGON2_PREFIX = "{argon2id}";

    private final UserRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredCredential> findByUsername(String username) {
        // Note: User entity's @SQLRestriction("deleted_at IS NULL") already filters soft-deleted
        // users at the JPA layer — findByEmailIgnoreCase only returns active rows.
        //
        // Stored hashes carry the DelegatingPasswordEncoder prefix ({argon2id}…) so legacy
        // bcrypt rows can coexist; secured-x's local provider, however, expects a raw PHC
        // string, so we strip the prefix here.
        return repo.findByEmailIgnoreCase(username)
                .map(
                        u ->
                                new StoredCredential(
                                        u.id().value(),
                                        u.email(),
                                        stripArgon2Prefix(u.passwordHash()),
                                        !u.enabled()));
    }

    private static String stripArgon2Prefix(String hash) {
        return hash != null && hash.startsWith(ARGON2_PREFIX)
                ? hash.substring(ARGON2_PREFIX.length())
                : hash;
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(String subject, Instant when) {
        repo.findById(subject).ifPresent(u -> u.recordLogin(when));
    }
}
