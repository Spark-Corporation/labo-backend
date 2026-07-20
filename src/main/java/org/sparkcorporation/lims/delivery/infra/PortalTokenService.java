package org.sparkcorporation.lims.delivery.infra;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Mints and verifies the second authentication realm's tokens: RS256 portal JWTs scoped to {@code
 * aud=lims-portal} with a 30-minute TTL. Reuses the existing dev RSA 2048-bit keys (configured at
 * {@code secured-x.jwt.private-key} / {@code secured-x.jwt.public-key}).
 *
 * <p>The Nimbus encoder/decoder are deliberately kept as <b>private fields</b>, never exposed as
 * {@code JwtEncoder} / {@code JwtDecoder} beans: the portal signing capability must stay
 * self-contained to this service and must not leak into the wider application context (where future
 * autoconfiguration could pick it up). The staff realm is owned entirely by {@code secured-x}.
 */
@Service
public class PortalTokenService {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String PORTAL_AUDIENCE = "lims-portal";
    private static final String ISSUER = "lims";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;

    public PortalTokenService(
            @Value("${secured-x.jwt.private-key}") Resource privateKeyPem,
            @Value("${secured-x.jwt.public-key}") Resource publicKeyPem,
            Clock clock)
            throws IOException {
        this.clock = clock;
        RSAPublicKey publicKey =
                (RSAPublicKey) RsaKeyConverters.x509().convert(publicKeyPem.getInputStream());
        RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(privateKeyPem.getInputStream());

        RSAKey jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        // The timestamp validator uses the injected Clock so expiry is evaluated against the same
        // clock that minted the token (deterministic under a fixed test Clock; real wall-clock in
        // production). Zero clock skew: the portal realm's tight 30-minute TTL must not be relaxed
        // by the default 60-second leniency. createDefaultWithValidators keeps the default
        // type/x509
        // validators and reuses our timestamp validator rather than adding a system-clock one.
        var timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(clock);
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withPublicKey(publicKey).build();
        nimbus.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithValidators(List.of(timestampValidator)),
                        new JwtClaimValidator<List<String>>(
                                JwtClaimNames.AUD,
                                aud -> aud != null && aud.contains(PORTAL_AUDIENCE)),
                        new JwtClaimValidator<String>("typ", "portal"::equals)));
        this.decoder = nimbus;
    }

    public PortalToken mint(PatientId patientId) {
        Instant now = clock.instant();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .audience(List.of(PORTAL_AUDIENCE))
                        .subject(patientId.value())
                        .issuedAt(now)
                        .expiresAt(now.plus(TTL))
                        .claim("typ", "portal")
                        .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new PortalToken(value, TTL.toSeconds());
    }

    /**
     * @throws org.springframework.security.oauth2.jwt.JwtException if invalid / expired / wrong
     *     audience
     */
    public PatientId verify(String token) {
        Jwt jwt = decoder.decode(token);
        return new PatientId(jwt.getSubject());
    }

    /**
     * A minted portal token plus its TTL in seconds (for the {@code expiresInSeconds} response).
     */
    public record PortalToken(String token, long expiresInSeconds) {}
}
