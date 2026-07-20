package org.sparkcorporation.lims.shared.money;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lab pricing config — shared across order (quote :lock) and billing (invoice issuance). Registered
 * via {@code @EnableConfigurationProperties} on the consuming module configs so the binder routes
 * through this compact constructor at startup. Failure mode is fail-fast (boot fails, not runtime
 * NPE inside {@code :lock} or invoice issuance).
 *
 * <p>{@code bRate} is the West African nomenclature pricing index ("valeur du B") used to compute
 * {@code lineTotal = indiciaryValueB × bRate × labelCount} — see order spec §5.3 and billing spec
 * §5.
 */
@ConfigurationProperties(prefix = "lims.pricing")
public record PricingProperties(BigDecimal bRate) {

    public PricingProperties {
        if (bRate == null || bRate.signum() <= 0) {
            throw new IllegalArgumentException("lims.pricing.b-rate must be > 0; got " + bRate);
        }
    }
}
