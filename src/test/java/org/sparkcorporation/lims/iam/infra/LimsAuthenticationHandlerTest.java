package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.sparksam.securedx.core.handler.AuthenticatedSubject;
import dev.sparksam.securedx.core.provider.AuthResult;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.iam.domain.User;

class LimsAuthenticationHandlerTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final LimsAuthenticationHandler handler = new LimsAuthenticationHandler(repo);

    @Test
    void onAuthenticated_returns_subject_with_user_ulid_and_no_claims() {
        var u = User.create("a@b.c", "Alice", "h", Set.of());
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));

        var result = new AuthResult(u.id().value(), u.email(), "local", Map.of());
        AuthenticatedSubject s = handler.onAuthenticated(result);

        assertThat(s.subject()).isEqualTo(u.id().value());
        assertThat(s.claims()).isEmpty();
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
