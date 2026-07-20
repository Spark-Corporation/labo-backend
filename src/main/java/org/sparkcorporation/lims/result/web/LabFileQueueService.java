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
import org.sparkcorporation.lims.result.infra.DossierRowProjection;
import org.sparkcorporation.lims.result.infra.OverdueDossierRowProjection;

@Service
@RequiredArgsConstructor
public class LabFileQueueService {

    private final DossierRepository repository;
    private final LabFileApi labFileApi;
    private final PatientApi patientApi;

    @Transactional(readOnly = true)
    public Page<DossierRow> findUrgent(Pageable pageable) {
        Page<DossierRowProjection> page = repository.findUrgent(pageable);
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

    @Transactional(readOnly = true)
    public Page<OverdueDossierRow> findOverdue(Pageable pageable) {
        Page<OverdueDossierRowProjection> page = repository.findOverdue(pageable);
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
                        DossierMapper.toOverdueRow(
                                p,
                                labFiles.get(new LabFileId(p.getLabFileId())),
                                patients.get(new PatientId(p.getPatientId()))));
    }
}
