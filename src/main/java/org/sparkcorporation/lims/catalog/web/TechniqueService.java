package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.TechniqueCreated;
import org.sparkcorporation.lims.catalog.api.TechniqueDisabled;
import org.sparkcorporation.lims.catalog.api.TechniqueId;
import org.sparkcorporation.lims.catalog.api.TechniqueUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Technique;
import org.sparkcorporation.lims.catalog.domain.TechniqueNotFoundException;
import org.sparkcorporation.lims.catalog.infra.TechniqueRepository;

/**
 * Application service for {@link Technique}. Implements the spec §7.5 idempotent-delete rule:
 * deleting an already soft-deleted technique is a no-op and emits no event.
 */
@Service
@RequiredArgsConstructor
public class TechniqueService {

    private final TechniqueRepository repo;
    private final TechniqueMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public TechniqueResponse create(CreateTechniqueRequest req) {
        if (repo.existsByCode(req.code())) {
            throw new CodeAlreadyExistsException("technique", req.code());
        }
        var t = Technique.create(req.code(), req.name(), req.description());
        repo.save(t);
        events.publishEvent(new TechniqueCreated(t.id(), t.code(), clock.instant()));
        return mapper.toResponse(t);
    }

    @Transactional(readOnly = true)
    public TechniqueResponse get(TechniqueId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new TechniqueNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<TechniqueResponse> search(boolean activeOnly, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Technique> result = activeOnly ? repo.findAllActive(pageable) : repo.findAll(pageable);
        return result.map(mapper::toResponse);
    }

    @Transactional
    public TechniqueResponse update(TechniqueId id, UpdateTechniqueRequest req) {
        var t = repo.findById(id.value()).orElseThrow(() -> new TechniqueNotFoundException(id));
        t.update(req.name(), req.description());
        events.publishEvent(new TechniqueUpdated(t.id(), clock.instant()));
        return mapper.toResponse(t);
    }

    @Transactional
    public void delete(TechniqueId id) {
        var t = repo.findById(id.value()).orElseThrow(() -> new TechniqueNotFoundException(id));
        if (!t.isActive()) {
            return; // idempotent — already soft-deleted, no-op (spec §7.5)
        }
        repo.delete(t);
        events.publishEvent(new TechniqueDisabled(t.id(), clock.instant()));
    }
}
