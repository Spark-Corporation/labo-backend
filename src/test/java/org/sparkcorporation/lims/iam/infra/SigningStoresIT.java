package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import dev.sparksam.securedx.core.signing.ActionSigner;
import dev.sparksam.securedx.core.signing.UserKeyPairManager;
import dev.sparksam.securedx.core.signing.UserKeyPairStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

@SpringBootTest
class SigningStoresIT extends IntegrationTestBase {

    @Autowired UserKeyPairStore keyPairStore;
    @Autowired UserKeyPairManager keyPairManager; // auto-wired because the two stores exist
    @Autowired ActionSigner actionSigner;

    @Autowired(required = false)
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void generate_key_then_sign_roundtrips_through_the_stores() {
        // seed a user row (subject FK target)
        String subject = Ulid.generate();
        jdbc.update(
                "INSERT INTO users (id, email, full_name, password_hash, enabled, version)"
                        + " VALUES (?, ?, 'KP Test', '{argon2id}x', true, 0)",
                subject,
                "kp-" + subject + "@test.local");

        keyPairManager.generateKeyPair(subject);
        assertThat(keyPairStore.getActive(subject)).isPresent();

        // sign as this subject via the security context — or use the explicit subject-bearing path
        var signed = signAs(subject, "test.action", "res-1", Map.of("k", "v"));
        assertThat(signed.sequenceNumber()).isEqualTo(1L);
        assertThat(signed.previousHash()).isNull(); // genesis
        assertThat(signed.signature()).isNotBlank();

        // private key is encrypted at rest
        byte[] stored =
                jdbc.queryForObject(
                        "SELECT private_key_encrypted FROM user_key_pairs WHERE subject=?",
                        byte[].class,
                        subject);
        byte[] plain = keyPairStore.getActive(subject).orElseThrow().privateKey();
        assertThat(stored).isNotEqualTo(plain);
    }

    // Helper: SecuredXSecurityContext.getCurrentSubject() reads
    // SecurityContextHolder.getContext().getAuthentication().getPrincipal() (a String) — there is
    // NO
    // setter on SecuredXSecurityContext. Populate the Spring SecurityContext so the signer resolves
    // the subject, then clear it.
    private dev.sparksam.securedx.core.signing.SignedAction signAs(
            String subject, String action, String resource, Map<String, Object> meta) {
        var auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        subject, null, java.util.List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(auth);
        try {
            return actionSigner.sign(action, resource, meta);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
