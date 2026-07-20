package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.iam.domain.User;
import org.sparkcorporation.lims.iam.domain.UserNotFoundException;

class DefaultUserApiTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final DefaultUserApi api = new DefaultUserApi(repo);

    private static User sample() {
        return User.create("alice@lab.tg", "Alice", "{argon2id}h", Set.of());
    }

    @Test
    void findById_returns_summary_when_present() {
        var u = sample();
        when(repo.findById(u.id().value())).thenReturn(Optional.of(u));
        var s = api.findById(u.id());
        assertThat(s).isPresent();
        assertThat(s.get().email()).isEqualTo("alice@lab.tg");
        assertThat(s.get().enabled()).isTrue();
    }

    @Test
    void findById_returns_empty_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(api.findById(UserId.next())).isEmpty();
    }

    @Test
    void requireById_throws_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(UserId.next()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void existsById_delegates_to_repo() {
        var id = UserId.next();
        when(repo.existsById(id.value())).thenReturn(true);
        assertThat(api.existsById(id)).isTrue();
    }
}
