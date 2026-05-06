package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.MatrixCreated;
import org.sparkcorporation.lims.catalog.api.MatrixDisabled;
import org.sparkcorporation.lims.catalog.api.MatrixId;
import org.sparkcorporation.lims.catalog.api.MatrixUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.MatrixNotFoundException;
import org.sparkcorporation.lims.catalog.infra.MatrixRepository;

/**
 * Application service for {@link Matrix}. Implements the spec §7.5 idempotent-delete rule: deleting
 * an already soft-deleted matrix is a no-op and emits no event.
 */
@Service
@RequiredArgsConstructor
public class MatrixService {

    private final MatrixRepository repo;
    private final MatrixMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public MatrixResponse create(CreateMatrixRequest req) {
        if (repo.existsByCode(req.code())) {
            throw new CodeAlreadyExistsException("matrix", req.code());
        }
        var m = Matrix.create(req.code(), req.name(), req.description());
        repo.save(m);
        events.publishEvent(new MatrixCreated(m.id(), m.code(), clock.instant()));
        return mapper.toResponse(m);
    }

    @Transactional(readOnly = true)
    public MatrixResponse get(MatrixId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new MatrixNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<MatrixResponse> search(boolean activeOnly, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Matrix> result = activeOnly ? repo.findAllActive(pageable) : repo.findAll(pageable);
        return result.map(mapper::toResponse);
    }

    @Transactional
    public MatrixResponse update(MatrixId id, UpdateMatrixRequest req) {
        var m = repo.findById(id.value()).orElseThrow(() -> new MatrixNotFoundException(id));
        m.update(req.name(), req.description());
        events.publishEvent(new MatrixUpdated(m.id(), clock.instant()));
        return mapper.toResponse(m);
    }

    @Transactional
    public void delete(MatrixId id) {
        var m = repo.findById(id.value()).orElseThrow(() -> new MatrixNotFoundException(id));
        if (!m.isActive()) {
            return; // idempotent — already soft-deleted, no-op (spec §7.5)
        }
        repo.delete(m);
        events.publishEvent(new MatrixDisabled(m.id(), clock.instant()));
    }
}
