package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import dev.sparksam.securedx.provider.local.StoredCredential;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.iam.domain.User;

class LimsLocalCredentialStoreTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final LimsLocalCredentialStore store = new LimsLocalCredentialStore(repo);

    private static User userFor(String email) {
        return User.create(email, "Alice", "{argon2id}fakehash", Set.of());
    }

    @Test
    void findByUsername_returns_credential_for_active_user() {
        var u = userFor("alice@lab.tg");
        when(repo.findByEmailIgnoreCase("alice@lab.tg")).thenReturn(Optional.of(u));
        Optional<StoredCredential> result = store.findByUsername("alice@lab.tg");
        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("alice@lab.tg");
        assertThat(result.get().subject()).isEqualTo(u.id().value());
        assertThat(result.get().argon2Hash()).isEqualTo("{argon2id}fakehash");
        assertThat(result.get().disabled()).isFalse();
    }

    @Test
    void findByUsername_returns_empty_for_unknown() {
        when(repo.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        assertThat(store.findByUsername("ghost@lab.tg")).isEmpty();
    }

    @Test
    void findByUsername_marks_disabled_when_user_not_enabled() {
        var u = userFor("a@b.c");
        u.disable();
        when(repo.findByEmailIgnoreCase("a@b.c")).thenReturn(Optional.of(u));
        assertThat(store.findByUsername("a@b.c").get().disabled()).isTrue();
    }

    @Test
    void recordSuccessfulLogin_updates_lastLoginAt() {
        var u = userFor("a@b.c");
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        var when = Instant.parse("2026-05-06T10:00:00Z");
        store.recordSuccessfulLogin(u.id().value(), when);
        assertThat(u.lastLoginAt()).isEqualTo(when);
    }

    @Test
    void recordSuccessfulLogin_noop_for_unknown_subject() {
        when(repo.findById(anyString())).thenReturn(Optional.empty());
        store.recordSuccessfulLogin("01HZB0000000000000000MSSGZ", Instant.now());
    }
}
