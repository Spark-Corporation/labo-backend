package org.sparkcorporation.lims.patient.infra;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Wires Spring Data JPA auditing for {@code @CreatedBy}/{@code @LastModifiedBy} fields.
 *
 * <p>The {@code AuditorAware} returns {@code Optional.empty()} until the iam sub-project (1.1.b)
 * lands a security-context-backed implementation. Until then, {@code created_by}/{@code updated_by}
 * stay null in the database — the columns are nullable in the Liquibase changeset.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "patientAuditorAware")
public class PatientJpaConfig {

    @Bean(name = "patientAuditorAware")
    public AuditorAware<String> patientAuditorAware() {
        return Optional::empty;
    }
}
