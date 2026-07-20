package org.sparkcorporation.lims.delivery.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.JwtException;

import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Unit test for the portal RS256 token service. Mints + verifies with the existing dev RSA keys
 * (loaded from the classpath) and a fixed {@link Clock} so the 30-minute TTL assertions are
 * deterministic.
 */
class PortalTokenServiceTest {

    private final PatientId patientId = new PatientId("01HZXAB0000000000000000001");

    private PortalTokenService serviceAt(Instant now) throws Exception {
        return new PortalTokenService(
                new ClassPathResource("keys/dev/private.pem"),
                new ClassPathResource("keys/dev/public.pem"),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void mint_then_verify_roundtrips_the_patient_id() throws Exception {
        var svc = serviceAt(Instant.parse("2026-05-28T10:00:00Z"));
        var token = svc.mint(patientId);
        assertThat(token.expiresInSeconds()).isEqualTo(1800);
        assertThat(svc.verify(token.token())).isEqualTo(patientId);
    }

    @Test
    void verify_rejects_an_expired_token() throws Exception {
        var minting = serviceAt(Instant.parse("2026-05-28T10:00:00Z"));
        var token = minting.mint(patientId);
        var later = serviceAt(Instant.parse("2026-05-28T10:31:00Z")); // > 30 min later
        assertThatThrownBy(() -> later.verify(token.token())).isInstanceOf(JwtException.class);
    }

    @Test
    void verify_rejects_a_tampered_token() throws Exception {
        var svc = serviceAt(Instant.parse("2026-05-28T10:00:00Z"));
        var token = svc.mint(patientId).token();
        assertThatThrownBy(() -> svc.verify(token + "x")).isInstanceOf(JwtException.class);
    }
}
