package org.sparkcorporation.lims.patient.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Public, read-only patient interface for cross-module consumption. Other modules import {@code
 * patient.api} only — they never touch {@code patient.domain} or {@code patient.infra} directly.
 */
public interface PatientApi {

    Optional<PatientSummary> findById(PatientId id);

    /**
     * Batched lookup. Returns a map keyed by the input ids; missing ids are silently absent from
     * the map. Empty input → empty map. Soft-deleted patients are excluded.
     *
     * <p>Added in 1.5.b for batched page-hydration in {@code
     * result/web/{ResultSearchService,DossierListService}} — avoids N+1 calls to {@link
     * #requireById(PatientId)} during paginated read endpoints.
     */
    Map<PatientId, PatientSummary> findAllByIds(Set<PatientId> ids);

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
