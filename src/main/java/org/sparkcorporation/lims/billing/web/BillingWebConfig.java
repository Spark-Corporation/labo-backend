package org.sparkcorporation.lims.billing.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.sparkcorporation.lims.billing.domain.PricingEngine;
import org.sparkcorporation.lims.billing.infra.LabProperties;
import org.sparkcorporation.lims.shared.money.PricingProperties;

/**
 * Billing-module Spring configuration. Registers {@link LabProperties} (the 3-letter lab code) and
 * {@link PricingProperties} (the {@code valeur du B}). {@link PricingProperties} is also registered
 * by {@code OrderConfig}; Spring tolerates the double registration since both point at the same
 * binding.
 *
 * <p>Also exposes {@link PricingEngine} as a Spring bean — it's a pure stateless domain service so
 * a singleton is sufficient and lets the {@code BillingFinalizationService} get it via constructor
 * injection.
 */
@Configuration
@EnableConfigurationProperties({LabProperties.class, PricingProperties.class})
public class BillingWebConfig {

    @Bean
    public PricingEngine pricingEngine() {
        return new PricingEngine();
    }
}
