package org.sparkcorporation.lims.patient.domain;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Pure domain service for merge invariants. Service-layer callers delegate here BEFORE mutating the
 * source aggregate, so failures map cleanly to HTTP status codes:
 *
 * <ul>
 *   <li>self-merge → {@link IllegalArgumentException} → 400 via {@code ProblemDetailAdvice}
 *   <li>already-merged source → {@link IllegalStateException} → caller maps to 409
 * </ul>
 *
 * <p>Deleted-patient cases are handled at the repository boundary (soft delete via
 * {@code @SQLRestriction} causes {@code findById} to return empty → {@code
 * PatientNotFoundException} → 404). This deliberately makes "merge into deleted target" a 404 not a
 * 409.
 */
@Component
public class PatientMerger {

    public void validateMerge(Patient source, PatientId targetId, Patient target) {
        if (source.id().equals(targetId)) {
            throw new IllegalArgumentException("cannot merge a patient into itself");
        }
        if (source.isMerged()) {
            throw new IllegalStateException(
                    "source patient already merged: " + source.id().value());
        }
    }
}
