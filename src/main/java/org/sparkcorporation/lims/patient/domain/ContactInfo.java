package org.sparkcorporation.lims.patient.domain;

import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

/**
 * Patient contact info (CDC §I "Numéro mobile et adresse Mail"). Both fields optional; format
 * validation happens at the request DTO via Bean Validation, not here.
 */
@Embeddable
public record ContactInfo(@Nullable String phone, @Nullable String email) {}
