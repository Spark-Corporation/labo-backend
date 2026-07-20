package org.sparkcorporation.lims.patient.api;

import org.jspecify.annotations.Nullable;

/**
 * Narrow, read-only contact projection of a patient for {@code notification} recipient resolution.
 * Deliberately separate from {@link PatientSummary}, which omits contact PII on purpose.
 *
 * <p><strong>PII — callers MUST NOT log any field of this record.</strong> Per the project logging
 * hygiene rules, patient email/phone never appear in log bodies; log the patient ULID instead.
 *
 * <p>{@code guardianPhone} is the fallback contact for the legal guardian (CDC §I), used when the
 * patient has no phone of their own.
 */
public record RecipientContact(
        @Nullable String email, @Nullable String phone, @Nullable String guardianPhone) {}
