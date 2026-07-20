package org.sparkcorporation.lims.result.api;

import java.util.List;
import java.util.Optional;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Public, read-only Result interface for cross-module consumption. Other modules import {@code
 * result.api} only — they never touch {@code result.domain} or {@code result.infra} directly.
 * Mirror of {@code order::api.LabFileApi} shape.
 */
public interface ResultApi {

    Optional<ResultSummary> findById(ResultId id);

    Optional<ResultSummary> findByAnalysisRequest(AnalysisRequestId id);

    /** Returns active (non-soft-deleted) results for a lab file, ordered by analysis code. */
    List<ResultSummary> findActiveByLabFile(LabFileId labFileId);

    /**
     * @throws org.sparkcorporation.lims.result.domain.ResultNotFoundException if no match
     */
    ResultSummary requireById(ResultId id);

    boolean existsById(ResultId id);
}
