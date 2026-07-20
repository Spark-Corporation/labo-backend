package org.sparkcorporation.lims.patient.infra;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.sparkcorporation.lims.patient.domain.LabCodePrefix;

/**
 * Reads {@code lims.lab.code-prefix} from configuration and exposes it as a typed {@link
 * LabCodePrefix} bean. Validation runs at startup — invalid prefix fails fast.
 */
@Configuration
@ConfigurationProperties("lims.lab")
public class PatientLabConfig {

    @NotBlank private String codePrefix = "LAB";

    public String getCodePrefix() {
        return codePrefix;
    }

    public void setCodePrefix(String codePrefix) {
        this.codePrefix = codePrefix;
    }

    @Bean
    public LabCodePrefix labCodePrefix() {
        return new LabCodePrefix(codePrefix);
    }
}
