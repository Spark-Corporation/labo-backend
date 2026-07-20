package org.sparkcorporation.lims.patient.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

import org.sparkcorporation.lims.patient.domain.LabCodePrefix;

class PatientLabConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ConfigurationPropertiesAutoConfiguration.class,
                                    ValidationAutoConfiguration.class))
                    .withUserConfiguration(PatientLabConfig.class);

    @Test
    void exposes_LabCodePrefix_bean_from_configured_value() {
        runner.withPropertyValues("lims.lab.code-prefix=LAB")
                .run(
                        ctx -> {
                            assertThat(ctx).hasSingleBean(LabCodePrefix.class);
                            assertThat(ctx.getBean(LabCodePrefix.class).value()).isEqualTo("LAB");
                        });
    }

    @Test
    void rejects_invalid_prefix_at_startup() {
        runner.withPropertyValues("lims.lab.code-prefix=lab")
                .run(
                        ctx -> {
                            assertThat(ctx).hasFailed();
                            assertThat(ctx.getStartupFailure())
                                    .hasMessageContaining("lab code prefix");
                        });
    }
}
