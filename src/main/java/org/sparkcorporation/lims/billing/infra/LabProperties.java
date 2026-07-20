package org.sparkcorporation.lims.billing.infra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds {@code lims.lab.code} — the 3-letter uppercase lab identifier used by {@link
 * InvoiceNumberGenerator} (e.g. {@code LTA} Togo, {@code LBA} Bénin). Fails boot if missing or
 * malformed so invoice issuance never NPEs at runtime.
 *
 * <p>The patient module's {@code PatientLabConfig} also binds under {@code lims.lab.code-prefix}
 * for patient-code generation — no conflict, different field.
 */
@ConfigurationProperties("lims.lab")
@Validated
public record LabProperties(@NotBlank @Pattern(regexp = "^[A-Z]{3}$") String code) {}
