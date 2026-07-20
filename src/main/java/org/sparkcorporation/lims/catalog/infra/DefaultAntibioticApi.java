package org.sparkcorporation.lims.catalog.infra;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AntibioticApi;
import org.sparkcorporation.lims.catalog.api.AntibioticCode;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.AntibioticSummary;
import org.sparkcorporation.lims.catalog.domain.Antibiotic;
import org.sparkcorporation.lims.catalog.domain.AntibioticNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAntibioticApi implements AntibioticApi {

    private final AntibioticRepository repo;

    @Override
    public Optional<AntibioticSummary> findById(AntibioticId id) {
        return repo.findById(id.value()).map(DefaultAntibioticApi::toSummary);
    }

    @Override
    public Map<AntibioticId, AntibioticSummary> findAllByIds(Collection<AntibioticId> ids) {
        Objects.requireNonNull(ids, "ids");
        if (ids.isEmpty()) {
            return Map.of();
        }
        var rawIds = ids.stream().map(AntibioticId::value).toList();
        var found = repo.findAllById(rawIds);
        var byId = new LinkedHashMap<AntibioticId, AntibioticSummary>(found.size());
        for (var a : found) {
            var summary = toSummary(a);
            byId.put(summary.id(), summary);
        }
        return byId;
    }

    @Override
    public Optional<AntibioticSummary> findByCode(AntibioticCode code) {
        return repo.findByCode(code.value()).map(DefaultAntibioticApi::toSummary);
    }

    @Override
    public AntibioticSummary requireById(AntibioticId id) {
        return repo.findById(id.value())
                .map(DefaultAntibioticApi::toSummary)
                .orElseThrow(() -> new AntibioticNotFoundException(id));
    }

    @Override
    public boolean existsById(AntibioticId id) {
        return repo.existsById(id.value());
    }

    @Override
    public List<AntibioticSummary> findAllActive() {
        return repo.findAllActiveOrderByCode().stream()
                .map(DefaultAntibioticApi::toSummary)
                .toList();
    }

    private static AntibioticSummary toSummary(Antibiotic a) {
        return new AntibioticSummary(
                a.id(),
                a.code(),
                a.name(),
                a.description(),
                a.family(),
                a.referenceDiameter(),
                a.intermediateLower(),
                a.intermediateUpper(),
                a.isActive());
    }
}
