package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.DisciplineApi;
import org.sparkcorporation.lims.catalog.api.DisciplineCode;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.DisciplineSummary;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.DisciplineNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultDisciplineApi implements DisciplineApi {

    private final DisciplineRepository repo;

    @Override
    public Optional<DisciplineSummary> findById(DisciplineId id) {
        return repo.findById(id.value()).map(DefaultDisciplineApi::toSummary);
    }

    @Override
    public Optional<DisciplineSummary> findByCode(DisciplineCode code) {
        return repo.findByCode(code.value()).map(DefaultDisciplineApi::toSummary);
    }

    @Override
    public DisciplineSummary requireById(DisciplineId id) {
        return repo.findById(id.value())
                .map(DefaultDisciplineApi::toSummary)
                .orElseThrow(() -> new DisciplineNotFoundException(id));
    }

    @Override
    public boolean existsById(DisciplineId id) {
        return repo.existsById(id.value());
    }

    @Override
    public List<DisciplineSummary> findAllActive() {
        return repo.findAllActiveOrderByCode().stream()
                .map(DefaultDisciplineApi::toSummary)
                .toList();
    }

    private static DisciplineSummary toSummary(Discipline d) {
        return new DisciplineSummary(
                d.id(), d.code(), d.name(), d.description(), d.isActive(), d.disciplinePrefix());
    }
}
