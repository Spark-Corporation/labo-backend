package org.sparkcorporation.lims.result.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.domain.Result;
import org.sparkcorporation.lims.result.infra.ResultRepository;

class ResultQueueServiceTest {

    private ResultRepository repository;
    private ResultQueueService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ResultRepository.class);
        service = new ResultQueueService(repository);
    }

    @Test
    void to_validate_returns_only_technical_validated_state() {
        var pageable = PageRequest.of(0, 20);
        Page<Result> empty = new PageImpl<>(List.of());
        Mockito.when(
                        repository.findActiveByStateOrderByTechnicalValidatedAt(
                                eq(ResultState.TECHNICAL_VALIDATED), eq(pageable)))
                .thenReturn(empty);

        var result = service.findToValidate(pageable);

        assertThat(result.getTotalElements()).isZero();
        Mockito.verify(repository)
                .findActiveByStateOrderByTechnicalValidatedAt(
                        ResultState.TECHNICAL_VALIDATED, pageable);
    }

    @Test
    void to_print_returns_only_biological_validated_state() {
        var pageable = PageRequest.of(0, 20);
        Mockito.when(
                        repository.findActiveByStateOrderByBiologicalValidatedAt(
                                eq(ResultState.BIOLOGICAL_VALIDATED), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        service.findToPrint(pageable);

        Mockito.verify(repository)
                .findActiveByStateOrderByBiologicalValidatedAt(
                        ResultState.BIOLOGICAL_VALIDATED, pageable);
    }
}
