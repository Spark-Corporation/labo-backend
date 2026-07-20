package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.SpecialtyCreated;
import org.sparkcorporation.lims.catalog.api.SpecialtyDisabled;
import org.sparkcorporation.lims.catalog.api.SpecialtyId;
import org.sparkcorporation.lims.catalog.api.SpecialtyUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.NewSpecialty;
import org.sparkcorporation.lims.catalog.domain.Specialty;
import org.sparkcorporation.lims.catalog.domain.SpecialtyNotFoundException;
import org.sparkcorporation.lims.catalog.domain.UpdateSpecialty;
import org.sparkcorporation.lims.catalog.infra.SpecialtyRepository;

/**
 * Application service for {@link Specialty}. Implements the spec §7.5 idempotent-delete rule:
 * deleting an already soft-deleted specialty is a no-op and emits no event.
 */
@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository repo;
    private final SpecialtyMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public SpecialtyResponse create(NewSpecialty cmd) {
        if (repo.findByCode(cmd.code()).isPresent()) {
            throw new CodeAlreadyExistsException("specialty", cmd.code());
        }
        var s = Specialty.create(cmd, clock);
        repo.save(s);
        events.publishEvent(new SpecialtyCreated(s.id(), s.code(), Instant.now(clock)));
        return mapper.toResponse(s);
    }

    @Transactional(readOnly = true)
    public SpecialtyResponse get(SpecialtyId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new SpecialtyNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<SpecialtyResponse> list(int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Specialty> result = repo.findAll(pageable);
        return result.map(mapper::toResponse);
    }

    @Transactional
    public SpecialtyResponse update(SpecialtyId id, UpdateSpecialty cmd) {
        var s = repo.findById(id.value()).orElseThrow(() -> new SpecialtyNotFoundException(id));
        s.updateNameAndDescription(cmd, clock);
        events.publishEvent(new SpecialtyUpdated(s.id(), Instant.now(clock)));
        return mapper.toResponse(s);
    }

    @Transactional
    public void delete(SpecialtyId id) {
        var s = repo.findById(id.value()).orElseThrow(() -> new SpecialtyNotFoundException(id));
        if (!s.isActive()) {
            return; // idempotent — already soft-deleted, no-op (spec §7.5)
        }
        repo.delete(s);
        events.publishEvent(new SpecialtyDisabled(s.id(), Instant.now(clock)));
    }
}
