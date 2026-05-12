package org.sparkcorporation.lims.result.web;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.result.infra.ResultSearchRepository;
import org.sparkcorporation.lims.result.infra.ResultSearchRowProjection;

@Service
@RequiredArgsConstructor
public class ResultSearchService {

    private final ResultSearchRepository repository;
    private final PatientApi patientApi;

    @Transactional(readOnly = true)
    public Page<ResultSearchRow> search(SearchFilters filters, Pageable pageable) {
        if (filters.dateFrom() != null
                && filters.dateTo() != null
                && filters.dateFrom().isAfter(filters.dateTo())) {
            throw new IllegalArgumentException(
                    "dateFrom must be <= dateTo (got dateFrom="
                            + filters.dateFrom()
                            + ", dateTo="
                            + filters.dateTo()
                            + ")");
        }

        Boolean insuredFilter =
                switch (filters.patientType()) {
                    case INSURED -> Boolean.TRUE;
                    case NOT_INSURED -> Boolean.FALSE;
                    case null -> null;
                };

        String stateEq = null;
        Boolean notValidated = null;
        Boolean urgentOnly = null;
        if (filters.resultState() != null) {
            switch (filters.resultState()) {
                case EMPTY -> stateEq = "DRAFT";
                case TO_CHECK -> stateEq = "ENTERED";
                case URGENT -> urgentOnly = Boolean.TRUE;
                case VALIDATED -> stateEq = "BIOLOGICAL_VALIDATED";
                case NOT_VALIDATED -> notValidated = Boolean.TRUE;
            }
        }

        var dateFrom =
                filters.dateFrom() == null
                        ? null
                        : ResultSearchRepository.startOfDay(filters.dateFrom());
        var dateTo =
                filters.dateTo() == null ? null : ResultSearchRepository.endOfDay(filters.dateTo());

        Page<ResultSearchRowProjection> page =
                repository.search(
                        dateFrom,
                        dateTo,
                        filters.analysisCode(),
                        insuredFilter,
                        stateEq,
                        notValidated,
                        urgentOnly,
                        pageable);

        Set<PatientId> patientIds =
                page.getContent().stream()
                        .map(p -> new PatientId(p.getPatientId()))
                        .collect(toUnmodifiableSet());
        var patients = patientApi.findAllByIds(patientIds);

        return page.map(
                row ->
                        ResultSearchMapper.toRow(
                                row, patients.get(new PatientId(row.getPatientId()))));
    }
}
