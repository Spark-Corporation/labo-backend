package org.sparkcorporation.lims.patient.infra;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public boolean existsById(PatientId id) {
        // Patient uses @SQLRestriction("deleted_at IS NULL") — findById auto-filters soft-deleted.
        return repo.findById(id.value()).isPresent();
    }

    @Override
    public Map<PatientId, PatientSummary> findAllByIds(Set<PatientId> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        var rawIds = ids.stream().map(PatientId::value).collect(Collectors.toSet());
        return repo.findAllActiveByIds(rawIds).stream()
                .collect(toUnmodifiableMap(p -> p.id(), DefaultPatientApi::toSummary));
    }

    private static PatientSummary toSummary(Patient p) {
        return new PatientSummary(
                p.id(),
                p.labCode(),
                p.name().lastName(),
                p.name().firstNames(),
                p.civility(),
                p.sex(),
                p.birthDate(),
                p.insured(),
                p.isMerged(),
                p.isDeleted());
    }
}
