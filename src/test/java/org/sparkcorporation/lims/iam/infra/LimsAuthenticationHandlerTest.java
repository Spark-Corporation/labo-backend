package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.sparksam.securedx.core.handler.AuthenticatedSubject;
import dev.sparksam.securedx.core.provider.AuthResult;
import dev.sparksam.securedx.core.signing.UserKeyPair;
import dev.sparksam.securedx.core.signing.UserKeyPairManager;
import dev.sparksam.securedx.core.signing.UserKeyPairStore;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.iam.domain.User;

class LimsAuthenticationHandlerTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final UserKeyPairStore keyPairStore = mock(UserKeyPairStore.class);
    private final UserKeyPairManager keyPairManager = mock(UserKeyPairManager.class);
    private final LimsAuthenticationHandler handler =
            new LimsAuthenticationHandler(repo, keyPairStore, keyPairManager);

    @Test
    void onAuthenticated_returns_subject_with_user_ulid_and_no_claims() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        when(keyPairStore.getActive(u.id().value())).thenReturn(Optional.empty());

        var result = new AuthResult(u.id().value(), u.email(), "local", Map.of());
        AuthenticatedSubject s = handler.onAuthenticated(result);

        assertThat(s.subject()).isEqualTo(u.id().value());
        assertThat(s.claims()).isEmpty();
    }

    @Test
    void onAuthenticated_provisions_a_key_pair_when_none_active() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        when(keyPairStore.getActive(u.id().value())).thenReturn(Optional.empty());

        handler.onAuthenticated(new AuthResult(u.id().value(), u.email(), "local", Map.of()));

        verify(keyPairManager).generateKeyPair(u.id().value());
    }

    @Test
    void onAuthenticated_skips_provisioning_when_a_key_pair_is_already_active() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        var existing =
                new UserKeyPair(
                        "key-1",
                        u.id().value(),
                        new byte[] {1},
                        new byte[] {2},
                        Instant.now(),
                        null,
                        true);
        when(keyPairStore.getActive(u.id().value())).thenReturn(Optional.of(existing));

        handler.onAuthenticated(new AuthResult(u.id().value(), u.email(), "local", Map.of()));

        verify(keyPairManager, never()).generateKeyPair(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void onAuthenticated_throws_when_user_vanished() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        var result = new AuthResult("01HZB0000000000000000SBJTZ", "a@b.c", "local", Map.of());
        assertThatThrownBy(() -> handler.onAuthenticated(result))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void onRefresh_true_when_user_enabled() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        assertThat(handler.onRefresh(u.id().value(), "any-jti")).isTrue();
    }

    @Test
    void onRefresh_false_when_user_disabled() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        u.disable();
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        assertThat(handler.onRefresh(u.id().value(), "any-jti")).isFalse();
    }

    @Test
    void onRefresh_false_when_user_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(handler.onRefresh("01HZB0000000000000000GHSTZ", "jti")).isFalse();
    }
}
