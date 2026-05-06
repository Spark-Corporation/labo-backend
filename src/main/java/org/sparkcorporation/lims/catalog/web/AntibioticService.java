package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AntibioticCreated;
import org.sparkcorporation.lims.catalog.api.AntibioticDisabled;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.AntibioticUpdated;
import org.sparkcorporation.lims.catalog.domain.Antibiotic;
import org.sparkcorporation.lims.catalog.domain.AntibioticNotFoundException;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.infra.AntibioticRepository;

/**
 * Application service for {@link Antibiotic}. Implements the spec §7.5 idempotent-delete rule:
 * deleting an already soft-deleted antibiotic is a no-op and emits no event.
 */
@Service
@RequiredArgsConstructor
public class AntibioticService {

    private final AntibioticRepository repo;
    private final AntibioticMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public AntibioticResponse create(CreateAntibioticRequest req) {
        if (repo.existsByCode(req.code())) {
            throw new CodeAlreadyExistsException("antibiotic", req.code());
        }
        var a =
                Antibiotic.create(
                        req.code(),
                        req.name(),
                        req.description(),
                        req.family(),
                        req.referenceDiameter(),
                        req.intermediateLower(),
                        req.intermediateUpper());
        repo.save(a);
        events.publishEvent(new AntibioticCreated(a.id(), a.code(), clock.instant()));
        return mapper.toResponse(a);
    }

    @Transactional(readOnly = true)
    public AntibioticResponse get(AntibioticId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new AntibioticNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<AntibioticResponse> search(boolean activeOnly, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Antibiotic> result =
                activeOnly ? repo.findAllActive(pageable) : repo.findAll(pageable);
        return result.map(mapper::toResponse);
    }

    @Transactional
    public AntibioticResponse update(AntibioticId id, UpdateAntibioticRequest req) {
        var a = repo.findById(id.value()).orElseThrow(() -> new AntibioticNotFoundException(id));
        a.update(
                req.name(),
                req.description(),
                req.family(),
                req.referenceDiameter(),
                req.intermediateLower(),
                req.intermediateUpper());
        events.publishEvent(new AntibioticUpdated(a.id(), clock.instant()));
        return mapper.toResponse(a);
    }

    @Transactional
    public void delete(AntibioticId id) {
        var a = repo.findById(id.value()).orElseThrow(() -> new AntibioticNotFoundException(id));
        if (!a.isActive()) {
            return; // idempotent — already soft-deleted, no-op (spec §7.5)
        }
        repo.delete(a);
        events.publishEvent(new AntibioticDisabled(a.id(), clock.instant()));
    }
}
