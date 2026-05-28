package org.sparkcorporation.lims.delivery.infra;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link PortalAuthFilter} scoped to {@code /api/v1/portal/*}. {@code secured-x} owns the
 * {@code SecurityFilterChain} and now treats {@code /api/v1/portal/**} as a public path (so the
 * staff chain waves it through); this path-scoped servlet filter is the actual gate. Instantiating
 * the filter only here (never as a {@code @Component}) avoids global {@code /*} registration and
 * keeps the portal realm out of {@code secured-x}'s internals.
 */
@Configuration
public class DeliverySecurityConfig {

    @Bean
    FilterRegistrationBean<PortalAuthFilter> portalAuthFilterRegistration(
            PortalTokenService tokenService) {
        var registration = new FilterRegistrationBean<>(new PortalAuthFilter(tokenService));
        registration.addUrlPatterns("/api/v1/portal/*");
        registration.setOrder(0); // after Spring Security's FilterChainProxy (order -100)
        return registration;
    }
}
