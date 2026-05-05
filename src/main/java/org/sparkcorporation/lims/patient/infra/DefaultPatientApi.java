package org.sparkcorporation.lims.patient.infra;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.PatientNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPatientApi implements PatientApi {

    private final PatientRepository repo;

    @Override
    public Optional<PatientSummary> findById(PatientId id) {
        return repo.findById(id.value()).map(DefaultPatientApi::toSummary);
    }

    @Override
    public PatientSummary requireById(PatientId id) {
        return repo.findById(id.value())
                .map(DefaultPatientApi::toSummary)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    @Override
    public PatientId followMergeChain(PatientId id) {
        return repo.findById(id.value())
                .map(p -> p.isMerged() ? new PatientId(p.mergedIntoId()) : id)
                .orElse(id);
    }

    private static PatientSummary toSummary(Patient p) {
        return new PatientSummary(
                p.id(),
                p.labCode(),
                p.name().lastName(),
                p.name().firstNames(),
                p.civility(),
                p.sex(),
                p.isMerged(),
                p.isDeleted());
    }
}
