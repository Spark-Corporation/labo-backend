package org.sparkcorporation.lims.order.web;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lab pricing config. Per spec §3 + §3.0 amendment registration: registered via {@link OrderConfig}
 * {@code @EnableConfigurationProperties} so the binder routes through this compact constructor at
 * startup. Failure mode is fail-fast (boot fails, not runtime NPE inside {@code :lock}).
 *
 * <p>{@code bRate} is the West African nomenclature pricing index ("valeur du B") used to compute
 * {@code lineTotal = indiciaryValueB × bRate × labelCount} — see spec §5.3.
 */
@ConfigurationProperties(prefix = "lims.pricing")
public record PricingProperties(BigDecimal bRate) {

    public PricingProperties {
        if (bRate == null || bRate.signum() <= 0) {
            throw new IllegalArgumentException("lims.pricing.b-rate must be > 0; got " + bRate);
        }
    }
}
