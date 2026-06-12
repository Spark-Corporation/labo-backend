package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import dev.sparksam.securedx.core.signing.ActionSigner;
import dev.sparksam.securedx.core.signing.UserKeyPairManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

@SpringBootTest
class AuditCheckpointServiceIT extends IntegrationTestBase {

    @Autowired private AuditCheckpointService service;
    @Autowired private SystemAuditIdentity identity;
    @Autowired private SignedActionRepository signedActions;
    @Autowired private UserKeyPairManager keyPairManager;
    @Autowired private ActionSigner actionSigner;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void builds_signs_and_persists_a_checkpoint_over_all_subject_heads() {
        String subjectA = seedSubjectWithKey();
        String subjectB = seedSubjectWithKey();
        signAs(subjectA, "test.a", "res-a", Map.of("k", "v"));
        signAs(subjectB, "test.b", "res-b", Map.of("k", "v"));

        var cp = service.createCheckpoint();

        assertThat(cp.chainHeads()).containsKeys(subjectA, subjectB);
        assertThat(cp.merkleRoot()).isEqualTo(AuditMerkle.root(cp.chainHeads()));
        assertThat(cp.globalSequence()).isEqualTo(signedActions.count());

        String payload =
                cp.checkpointId()
                        + "|"
                        + cp.globalSequence()
                        + "|"
                        + cp.timestamp()
                        + "|"
                        + cp.merkleRoot()
                        + "|"
                        + AuditMerkle.canonicalJson(cp.chainHeads());
        assertThat(identity.verify(payload.getBytes(StandardCharsets.UTF_8), cp.signature()))
                .isTrue();
    }

    private String seedSubjectWithKey() {
        String subject = Ulid.generate();
        jdbc.update(
                "INSERT INTO users (id, email, full_name, password_hash, enabled, version)"
                        + " VALUES (?, ?, 'CP Test', '{argon2id}x', true, 0)",
                subject,
                "cp-" + subject + "@test.local");
        keyPairManager.generateKeyPair(subject);
        return subject;
    }

    private void signAs(String subject, String action, String resource, Map<String, Object> meta) {
        var auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        subject, null, List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(auth);
        try {
            actionSigner.sign(action, resource, meta);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
