package org.sparkcorporation.lims.order.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.sparkcorporation.lims.order.domain.AnalysisRequest;

/**
 * Spring Data JPA repository for {@link AnalysisRequest}, owned by {@code LabFile}.
 *
 * <p>{@link #findByLabFileId(String)} is the third round-trip of the multi-bag-fetch split — see
 * {@link LabFileRepository} class-doc and spec §6.3. {@link #existsByLabFileIdAndAnalysisId(String,
 * String)} backs the de-duplication invariant on the {@code (lab_file_id, analysis_id)} unique
 * constraint.
 */
public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, String> {

    List<AnalysisRequest> findByLabFileId(String labFileId);

    boolean existsByLabFileIdAndAnalysisId(String labFileId, String analysisId);
}
