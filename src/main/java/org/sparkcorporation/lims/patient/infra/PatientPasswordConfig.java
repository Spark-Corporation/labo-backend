package org.sparkcorporation.lims.patient.infra;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides a {@link PasswordEncoder} for hashing patient portal passwords and a {@link Clock} bean
 * for deterministic time access.
 *
 * <p>The encoder uses Spring Security's {@code DelegatingPasswordEncoder} which prefixes hashes
 * with the algorithm id (e.g. {@code {bcrypt}…}). This allows transparent migration from bcrypt to
 * argon2id once secured-x's {@code Argon2idHasher} ships in sub-project 1.1.a.
 *
 * <p>The {@code Clock} bean is the system default zone clock at runtime; tests inject {@link
 * Clock#fixed} for deterministic time. {@code @ConditionalOnMissingBean} allows tests/other configs
 * to override it without bean-definition conflicts.
 */
@Configuration
public class PatientPasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
