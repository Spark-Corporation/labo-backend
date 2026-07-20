package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.PreleveurApi;
import org.sparkcorporation.lims.catalog.api.PreleveurCode;
import org.sparkcorporation.lims.catalog.api.PreleveurId;
import org.sparkcorporation.lims.catalog.api.PreleveurSummary;
import org.sparkcorporation.lims.catalog.domain.Preleveur;
import org.sparkcorporation.lims.catalog.domain.PreleveurNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPreleveurApi implements PreleveurApi {

    private final PreleveurRepository repo;

    @Override
    public Optional<PreleveurSummary> findById(PreleveurId id) {
        return repo.findById(id.value()).map(DefaultPreleveurApi::toSummary);
    }

    @Override
    public Optional<PreleveurSummary> findByCode(PreleveurCode code) {
        return repo.findByCode(code.value()).map(DefaultPreleveurApi::toSummary);
    }

    @Override
    public PreleveurSummary requireById(PreleveurId id) {
        return repo.findById(id.value())
                .map(DefaultPreleveurApi::toSummary)
                .orElseThrow(() -> new PreleveurNotFoundException(id));
    }

    @Override
    public boolean existsById(PreleveurId id) {
        return repo.existsById(id.value());
    }

    @Override
    public List<PreleveurSummary> findAllActive() {
        return repo.findAllActive().stream().map(DefaultPreleveurApi::toSummary).toList();
    }

    private static PreleveurSummary toSummary(Preleveur p) {
        return new PreleveurSummary(
                p.id(), p.code(), p.name(), p.phone(), p.email(), p.userId(), p.habilitations());
    }
}
