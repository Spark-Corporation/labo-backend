package org.sparkcorporation.lims.order.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import org.sparkcorporation.lims.order.infra.ConservationAlertProperties;
import org.sparkcorporation.lims.shared.money.PricingProperties;

/**
 * Order-module Spring configuration. Registers {@link PricingProperties} so the binder routes
 * through its compact constructor at startup — a missing or non-positive {@code
 * lims.pricing.b-rate} fails the boot deterministically rather than NPE-ing the first {@code :lock}
 * call. Also registers {@link ConservationAlertProperties} for the SP9 destruction-alert scan.
 */
@Configuration
@EnableConfigurationProperties({PricingProperties.class, ConservationAlertProperties.class})
public class OrderConfig {}
