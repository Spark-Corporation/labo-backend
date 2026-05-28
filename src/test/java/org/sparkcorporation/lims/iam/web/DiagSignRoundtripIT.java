package org.sparkcorporation.lims.iam.web;

import java.util.Map;

import dev.sparksam.securedx.core.signing.ActionSigner;
import dev.sparksam.securedx.core.signing.AuditChainStore;
import dev.sparksam.securedx.core.signing.UserKeyPairManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

@SpringBootTest
class DiagSignRoundtripIT extends IntegrationTestBase {

    @Autowired UserKeyPairManager keyPairManager;
    @Autowired ActionSigner signer;
    @Autowired AuditChainStore store;
    @Autowired JdbcTemplate jdbc;

    @Test
    void diag() {
        String subject = Ulid.generate();
        jdbc.update(
                "INSERT INTO users (id, email, full_name, password_hash, enabled, version)"
                        + " VALUES (?, ?, 'Diag', '{argon2id}x', true, 0)",
                subject,
                "diag-" + subject + "@test.local");
        keyPairManager.generateKeyPair(subject);
        var auth = new UsernamePasswordAuthenticationToken(subject, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            var signed = signer.sign("diag.action", "/diag", Map.of("k", "v"));
            System.out.println("SIGNED_TS=[" + signed.timestamp() + "]");
            var reloaded = store.getBySequence(subject, signed.sequenceNumber()).orElseThrow();
            System.out.println("RELOAD_TS=[" + reloaded.timestamp() + "]");
            System.out.println("TS_EQUAL=" + signed.timestamp().equals(reloaded.timestamp()));
            System.out.println("META_SIGNED=" + signed.metadata());
            System.out.println("META_RELOAD=" + reloaded.metadata());
            System.out.println("META_EQUAL=" + signed.metadata().equals(reloaded.metadata()));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
