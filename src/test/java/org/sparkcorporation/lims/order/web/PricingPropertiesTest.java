package org.sparkcorporation.lims.order.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Context-loading test that proves the Spring binder routes through {@link PricingProperties}'
 * compact constructor at startup. Uses {@link ApplicationContextRunner} (no full
 * {@code @SpringBootTest} bootstrap) so the binder behavior is verified without spinning the rest
 * of the Spring context.
 */
class PricingPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void binds_b_rate_when_configured() {
        runner.withPropertyValues("lims.pricing.b-rate=400")
                .run(
                        ctx -> {
                            var props = ctx.getBean(PricingProperties.class);
                            assertThat(props.bRate()).isEqualByComparingTo(new BigDecimal("400"));
                        });
    }

    @Test
    void fails_fast_when_b_rate_missing() {
        runner.run(
                ctx ->
                        assertThat(ctx)
                                .hasFailed()
                                .getFailure()
                                .isInstanceOf(BeanCreationException.class)
                                .rootCause()
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("b-rate"));
    }

    @Test
    void fails_fast_when_b_rate_zero_or_negative() {
        runner.withPropertyValues("lims.pricing.b-rate=0").run(ctx -> assertThat(ctx).hasFailed());
        runner.withPropertyValues("lims.pricing.b-rate=-1").run(ctx -> assertThat(ctx).hasFailed());
    }

    @EnableConfigurationProperties(PricingProperties.class)
    static class TestConfig {}
}
