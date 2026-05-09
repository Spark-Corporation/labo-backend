package org.sparkcorporation.lims.patient.api;

import java.util.Optional;

/**
 * Public, read-only patient interface for cross-module consumption. Other modules import {@code
 * patient.api} only — they never touch {@code patient.domain} or {@code patient.infra} directly.
 */
public interface PatientApi {

    Optional<PatientSummary> findById(PatientId id);

    /**
     * @throws org.sparkcorporation.lims.patient.domain.PatientNotFoundException if no match
     */
    PatientSummary requireById(PatientId id);

    /**
     * Returns the surviving target id if {@code id} has been merged; else returns {@code id}
     * itself.
     */
    PatientId followMergeChain(PatientId id);

    /**
     * Active-only existence check (filters soft-deleted via {@code @SQLRestriction}). Returns
     * {@code false} when the id is unknown or the patient row has been soft-deleted.
     */
    boolean existsById(PatientId id);
}
