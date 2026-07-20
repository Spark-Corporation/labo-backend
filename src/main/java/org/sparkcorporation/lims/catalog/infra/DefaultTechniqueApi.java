package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.TechniqueApi;
import org.sparkcorporation.lims.catalog.api.TechniqueCode;
import org.sparkcorporation.lims.catalog.api.TechniqueId;
import org.sparkcorporation.lims.catalog.api.TechniqueSummary;
import org.sparkcorporation.lims.catalog.domain.Technique;
import org.sparkcorporation.lims.catalog.domain.TechniqueNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultTechniqueApi implements TechniqueApi {

    private final TechniqueRepository repo;

    @Override
    public Optional<TechniqueSummary> findById(TechniqueId id) {
        return repo.findById(id.value()).map(DefaultTechniqueApi::toSummary);
    }

    @Override
    public Optional<TechniqueSummary> findByCode(TechniqueCode code) {
        return repo.findByCode(code.value()).map(DefaultTechniqueApi::toSummary);
    }

    @Override
    public TechniqueSummary requireById(TechniqueId id) {
        return repo.findById(id.value())
                .map(DefaultTechniqueApi::toSummary)
                .orElseThrow(() -> new TechniqueNotFoundException(id));
    }

    @Override
    public boolean existsById(TechniqueId id) {
        return repo.existsById(id.value());
    }

    @Override
    public List<TechniqueSummary> findAllActive() {
        return repo.findAllActiveOrderByCode().stream()
                .map(DefaultTechniqueApi::toSummary)
                .toList();
    }

    private static TechniqueSummary toSummary(Technique t) {
        return new TechniqueSummary(t.id(), t.code(), t.name(), t.description(), t.isActive());
    }
}
