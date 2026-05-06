package org.sparkcorporation.lims.patient.infra;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides a {@link Clock} bean for deterministic time access. Patient module owns this bean. */
@Configuration
public class PatientPasswordConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
