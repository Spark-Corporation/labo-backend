package org.sparkcorporation.lims.result.web;

import static java.util.stream.Collectors.toUnmodifiableSet;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.result.infra.DossierRepository;
import org.sparkcorporation.lims.result.infra.ResultSearchRepository;

@Service
@RequiredArgsConstructor
public class DossierListService {

    private final DossierRepository repository;
    private final LabFileApi labFileApi;
    private final PatientApi patientApi;

    @Transactional(readOnly = true)
    public Page<DossierRow> list(DossierFilters filters, Pageable pageable) {
        Boolean validatedOnly = null;
        Boolean notValidated = null;
        Boolean inProgress = null;
        Boolean includeDeleted = null;
        Boolean deletedOnly = null;
        if (filters.state() != null) {
            switch (filters.state()) {
                case VALIDATED -> validatedOnly = Boolean.TRUE;
                case NOT_VALIDATED -> notValidated = Boolean.TRUE;
                case IN_PROGRESS -> inProgress = Boolean.TRUE;
                case DELETED -> {
                    includeDeleted = Boolean.TRUE;
                    deletedOnly = Boolean.TRUE;
                }
            }
        }

        var dateFrom =
                filters.dateFrom() == null
                        ? null
                        : ResultSearchRepository.startOfDay(filters.dateFrom());
        var dateTo =
                filters.dateTo() == null ? null : ResultSearchRepository.endOfDay(filters.dateTo());

        var page =
                repository.list(
                        includeDeleted,
                        deletedOnly,
                        filters.urgent(),
                        filters.empty(),
                        validatedOnly,
                        notValidated,
                        inProgress,
                        filters.printed(),
                        filters.signed(),
                        filters.emailed(),
                        filters.patientId(),
                        dateFrom,
                        dateTo,
                        pageable);

        var labFileIds =
                page.getContent().stream()
                        .map(p -> new LabFileId(p.getLabFileId()))
                        .collect(toUnmodifiableSet());
        var patientIds =
                page.getContent().stream()
                        .map(p -> new PatientId(p.getPatientId()))
                        .collect(toUnmodifiableSet());
        var labFiles = labFileApi.findAllByIds(labFileIds);
        var patients = patientApi.findAllByIds(patientIds);

        return page.map(
                p ->
                        DossierMapper.toRow(
                                p,
                                labFiles.get(new LabFileId(p.getLabFileId())),
                                patients.get(new PatientId(p.getPatientId()))));
    }
}
