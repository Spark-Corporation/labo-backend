package org.sparkcorporation.lims.iam.infra;

import dev.sparksam.securedx.core.security.SecuredXSecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Wires global JPA auditing and contributes a SecuredXSecurityCustomizer for LIMS-specific
 * HttpSecurity tweaks (e.g. propagating AccessDeniedException so ProblemDetailAdvice maps it).
 *
 * <p>Does NOT declare a SecurityFilterChain — secured-x-spring-boot-starter's
 * SecuredXSecurityAutoConfiguration handles that under {@code @ConditionalOnMissingBean}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class IamSecurityConfig {

    @Bean
    public SecuredXSecurityCustomizer limsSecurityCustomizer() {
        return http ->
                http.exceptionHandling(
                        eh ->
                                eh.accessDeniedHandler(
                                        (req, res, ex) -> {
                                            throw ex;
                                        }));
    }
}
