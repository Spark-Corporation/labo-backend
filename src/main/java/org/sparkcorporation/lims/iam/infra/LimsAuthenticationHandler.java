package org.sparkcorporation.lims.iam.infra;

import java.util.Map;

import dev.sparksam.securedx.core.handler.AuthenticatedSubject;
import dev.sparksam.securedx.core.handler.AuthenticationHandler;
import dev.sparksam.securedx.core.provider.AuthResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LimsAuthenticationHandler implements AuthenticationHandler {

    private final UserRepository repo;

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedSubject onAuthenticated(AuthResult result) {
        var user =
                repo.findById(result.externalId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "authenticated subject vanished: "
                                                        + result.externalId()));
        return new AuthenticatedSubject(user.id().value(), Map.of());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean onRefresh(String subject, String jti) {
        return repo.findById(subject).filter(u -> u.enabled()).isPresent();
    }

    @Override
    public void onRevoke(String subject) {
        // no-op: revocation list deferred per spec §2.2
    }
}
