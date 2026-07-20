package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.SpecialtyApi;
import org.sparkcorporation.lims.catalog.api.SpecialtyCode;
import org.sparkcorporation.lims.catalog.api.SpecialtyId;
import org.sparkcorporation.lims.catalog.api.SpecialtySummary;
import org.sparkcorporation.lims.catalog.domain.Specialty;
import org.sparkcorporation.lims.catalog.domain.SpecialtyNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultSpecialtyApi implements SpecialtyApi {

    private final SpecialtyRepository repo;

    @Override
    public Optional<SpecialtySummary> findById(SpecialtyId id) {
        return repo.findById(id.value()).map(DefaultSpecialtyApi::toSummary);
    }

    @Override
    public Optional<SpecialtySummary> findByCode(SpecialtyCode code) {
        return repo.findByCode(code.value()).map(DefaultSpecialtyApi::toSummary);
    }

    @Override
    public SpecialtySummary requireById(SpecialtyId id) {
        return repo.findById(id.value())
                .map(DefaultSpecialtyApi::toSummary)
                .orElseThrow(() -> new SpecialtyNotFoundException(id));
    }

    @Override
    public boolean existsById(SpecialtyId id) {
        return repo.existsById(id.value());
    }

    @Override
    public List<SpecialtySummary> findAllActive() {
        return repo.findAllActive().stream().map(DefaultSpecialtyApi::toSummary).toList();
    }

    private static SpecialtySummary toSummary(Specialty s) {
        return new SpecialtySummary(s.id(), s.code(), s.name(), s.description());
    }
}
