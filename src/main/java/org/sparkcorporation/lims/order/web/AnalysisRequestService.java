package org.sparkcorporation.lims.order.web;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.NewAnalysisRequest;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.infra.LabFileRepository;

/**
 * Application service for adding / removing {@code AnalysisRequest} rows on an existing OPEN
 * lab-file. Per spec §7.1: bulk-add returns 200 with the updated full {@link LabFileResponse}
 * (mutating an existing parent), and a transactional rollback covers the entire batch when any code
 * fails resolution.
 */
@Service
@RequiredArgsConstructor
public class AnalysisRequestService {

    private final LabFileRepository fileRepo;
    private final AnalysisApi analysisApi;
    private final LabFileApi labFileApi;
    private final Clock clock;

    @Transactional
    public LabFileResponse addAnalyses(LabFileId fileId, List<String> codes) {
        var file =
                fileRepo.findActiveById(fileId.value())
                        .orElseThrow(() -> new LabFileNotFoundException(fileId));
        var seen = new HashSet<String>();
        for (var code : codes) {
            if (!seen.add(code)) {
                throw new DuplicateAnalysisRequestException(code);
            }
            var summary =
                    analysisApi
                            .findByCode(new AnalysisCode(code))
                            .orElseThrow(
                                    () ->
                                            new OrderInvalidAnalysisReferenceException(
                                                    code, "not found"));
            file.addAnalysisRequest(new NewAnalysisRequest(summary.id().value(), code), clock);
        }
        return LabFileMapper.toResponse(labFileApi.requireById(fileId));
    }

    @Transactional
    public void removeAnalysis(LabFileId fileId, AnalysisRequestId requestId) {
        var file =
                fileRepo.findActiveById(fileId.value())
                        .orElseThrow(() -> new LabFileNotFoundException(fileId));
        file.removeAnalysisRequest(requestId, clock);
    }
}
