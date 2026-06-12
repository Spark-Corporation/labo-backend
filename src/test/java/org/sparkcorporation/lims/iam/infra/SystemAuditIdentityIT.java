package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.sparkcorporation.lims.IntegrationTestBase;

@SpringBootTest
class SystemAuditIdentityIT extends IntegrationTestBase {

    @Autowired private SystemAuditIdentity identity;

    @Test
    void provisions_a_system_key_and_round_trips_a_signature() {
        byte[] payload = "checkpoint-payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String keyId = identity.ensureProvisioned();
        String sig = identity.sign(payload);

        assertThat(keyId).isNotBlank();
        assertThat(identity.verify(payload, sig)).isTrue();
        assertThat(identity.verify("tampered".getBytes(), sig)).isFalse();
    }

    @Test
    void provisioning_is_idempotent() {
        String first = identity.ensureProvisioned();
        String second = identity.ensureProvisioned();
        assertThat(second).isEqualTo(first);
    }
}
