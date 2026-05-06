package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class LimsAuditorAwareTest {

    private final LimsAuditorAware aware = new LimsAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void empty_when_no_authentication() {
        SecurityContextHolder.clearContext();
        assertThat(aware.getCurrentAuditor()).isEmpty();
    }

    @Test
    void empty_when_anonymous_principal() {
        var anon =
                new AnonymousAuthenticationToken(
                        "k",
                        "anonymousUser",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);
        assertThat(aware.getCurrentAuditor()).isEmpty();
    }

    @Test
    void returns_user_ulid_when_authenticated() {
        var auth =
                new UsernamePasswordAuthenticationToken(
                        "01HZB0000000000000000SBJTZ",
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(aware.getCurrentAuditor()).contains("01HZB0000000000000000SBJTZ");
    }

    @Test
    void empty_when_authentication_isAuthenticated_returns_false() {
        var auth = new UsernamePasswordAuthenticationToken(null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(aware.getCurrentAuditor()).isEmpty();
    }
}
