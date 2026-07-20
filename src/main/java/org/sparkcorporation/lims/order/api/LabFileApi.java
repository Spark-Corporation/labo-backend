package org.sparkcorporation.lims.order.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Public, read-only LabFile interface for cross-module consumption. Other modules import {@code
 * order.api} only — they never reach into {@code order.domain} or {@code order.infra} directly.
 */
public interface LabFileApi {

    Optional<LabFileSummary> findById(LabFileId id);

    Optional<LabFileSummary> findByRegistrationNumber(LabFileRegistrationNumber rn);

    /**
     * @throws org.sparkcorporation.lims.order.domain.LabFileNotFoundException if no match
     */
    LabFileSummary requireById(LabFileId id);

    boolean existsById(LabFileId id);

    /** Returns active (non-deleted) lab files for a patient, ordered by {@code createdAt} desc. */
    List<LabFileSummary> findActiveByPatient(String patientId);

    /**
     * Batched lookup. Returns a map keyed by the input ids; missing ids are silently absent. Empty
     * input → empty map. Soft-deleted lab-files are excluded.
     *
     * <p>Added in 1.5.b for batched page-hydration in {@code
     * result/web/{LabFileQueueService,DossierListService}}.
     */
    Map<LabFileId, LabFileSummary> findAllByIds(Set<LabFileId> ids);
}
