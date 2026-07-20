package org.sparkcorporation.lims.iam.infra;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Argon2id-default {@link DelegatingPasswordEncoder}. New hashes use {@code {argon2id}…}; legacy
 * {@code {bcrypt}…} hashes still verify on login.
 */
@Configuration
public class IamPasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("argon2id", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        return new DelegatingPasswordEncoder("argon2id", encoders);
    }
}
