package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.DisciplineCreated;
import org.sparkcorporation.lims.catalog.api.DisciplineDisabled;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.DisciplineUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.DisciplineNotFoundException;
import org.sparkcorporation.lims.catalog.infra.DisciplineRepository;

/**
 * Application service for {@link Discipline}. Implements the spec §7.5 idempotent-delete rule:
 * deleting an already soft-deleted discipline is a no-op and emits no event.
 */
@Service
@RequiredArgsConstructor
public class DisciplineService {

    private final DisciplineRepository repo;
    private final DisciplineMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public DisciplineResponse create(CreateDisciplineRequest req) {
        if (repo.existsByCode(req.code())) {
            throw new CodeAlreadyExistsException("discipline", req.code());
        }
        var d = Discipline.create(req.code(), req.name(), req.description());
        repo.save(d);
        events.publishEvent(new DisciplineCreated(d.id(), d.code(), clock.instant()));
        return mapper.toResponse(d);
    }

    @Transactional(readOnly = true)
    public DisciplineResponse get(DisciplineId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new DisciplineNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<DisciplineResponse> search(boolean activeOnly, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Discipline> result =
                activeOnly ? repo.findAllActive(pageable) : repo.findAll(pageable);
        return result.map(mapper::toResponse);
    }

    @Transactional
    public DisciplineResponse update(DisciplineId id, UpdateDisciplineRequest req) {
        var d = repo.findById(id.value()).orElseThrow(() -> new DisciplineNotFoundException(id));
        d.update(req.name(), req.description());
        events.publishEvent(new DisciplineUpdated(d.id(), clock.instant()));
        return mapper.toResponse(d);
    }

    @Transactional
    public void delete(DisciplineId id) {
        var d = repo.findById(id.value()).orElseThrow(() -> new DisciplineNotFoundException(id));
        if (!d.isActive()) {
            return; // idempotent — already soft-deleted, no-op (spec §7.5)
        }
        repo.delete(d);
        events.publishEvent(new DisciplineDisabled(d.id(), clock.instant()));
    }
}
