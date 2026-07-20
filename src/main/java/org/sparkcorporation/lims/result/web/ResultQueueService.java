package org.sparkcorporation.lims.result.web;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.ResultSummary;
import org.sparkcorporation.lims.result.infra.ResultRepository;
import org.sparkcorporation.lims.result.infra.ResultSummaryMapper;

@Service
@RequiredArgsConstructor
public class ResultQueueService {

    private final ResultRepository repository;

    @Transactional(readOnly = true)
    public Page<ResultSummary> findToValidate(Pageable pageable) {
        return repository
                .findActiveByStateOrderByTechnicalValidatedAt(
                        ResultState.TECHNICAL_VALIDATED, pageable)
                .map(ResultSummaryMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<ResultSummary> findToPrint(Pageable pageable) {
        return repository
                .findActiveByStateOrderByBiologicalValidatedAt(
                        ResultState.BIOLOGICAL_VALIDATED, pageable)
                .map(ResultSummaryMapper::toSummary);
    }
}
