package org.sparkcorporation.lims.iam.infra;

import java.util.Map;

import dev.sparksam.securedx.core.handler.AuthenticatedSubject;
import dev.sparksam.securedx.core.handler.AuthenticationHandler;
import dev.sparksam.securedx.core.provider.AuthResult;
import dev.sparksam.securedx.core.signing.UserKeyPairManager;
import dev.sparksam.securedx.core.signing.UserKeyPairStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LimsAuthenticationHandler implements AuthenticationHandler {

    private final UserRepository repo;
    private final UserKeyPairStore keyPairStore; // secured-x SPI (our bean)
    private final UserKeyPairManager keyPairManager; // secured-x, auto-wired

    @Override
    @Transactional // writable now (was readOnly) — provisioning writes a key pair
    public AuthenticatedSubject onAuthenticated(AuthResult result) {
        var user =
                repo.findById(result.externalId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "authenticated subject vanished: "
                                                        + result.externalId()));
        String subject = user.id().value();
        if (keyPairStore.getActive(subject).isEmpty()) {
            keyPairManager.generateKeyPair(subject); // persists via LimsUserKeyPairStore
        }
        return new AuthenticatedSubject(subject, Map.of());
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
