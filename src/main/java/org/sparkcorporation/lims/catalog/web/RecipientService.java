package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.MatrixId;
import org.sparkcorporation.lims.catalog.api.RecipientCreated;
import org.sparkcorporation.lims.catalog.api.RecipientDisabled;
import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.api.RecipientUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.MatrixNotFoundException;
import org.sparkcorporation.lims.catalog.domain.Recipient;
import org.sparkcorporation.lims.catalog.domain.RecipientNotFoundException;
import org.sparkcorporation.lims.catalog.infra.MatrixRepository;
import org.sparkcorporation.lims.catalog.infra.RecipientRepository;

/**
 * Application service for {@link Recipient}. Resolves {@code defaultMatrixId} (raw ULID string) to
 * a {@link Matrix} aggregate before invoking the domain factory. Implements the spec §7.5
 * idempotent-delete rule.
 */
@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository repo;
    private final MatrixRepository matrixRepo;
    private final RecipientMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public RecipientResponse create(CreateRecipientRequest req) {
        if (repo.existsByCode(req.code())) {
            throw new CodeAlreadyExistsException("recipient", req.code());
        }
        Matrix matrix = resolveMatrix(req.defaultMatrixId());
        var r =
                Recipient.create(
                        req.code(),
                        req.name(),
                        req.description(),
                        req.color(),
                        req.volumeMl(),
                        matrix);
        repo.save(r);
        events.publishEvent(new RecipientCreated(r.id(), r.code(), clock.instant()));
        return mapper.toResponse(r);
    }

    @Transactional(readOnly = true)
    public RecipientResponse get(RecipientId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new RecipientNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<RecipientResponse> search(boolean activeOnly, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Recipient> result = activeOnly ? repo.findAllActive(pageable) : repo.findAll(pageable);
        return result.map(mapper::toResponse);
    }

    @Transactional
    public RecipientResponse update(RecipientId id, UpdateRecipientRequest req) {
        var r = repo.findById(id.value()).orElseThrow(() -> new RecipientNotFoundException(id));
        Matrix matrix = resolveMatrix(req.defaultMatrixId());
        r.update(req.name(), req.description(), req.color(), req.volumeMl(), matrix);
        events.publishEvent(new RecipientUpdated(r.id(), clock.instant()));
        return mapper.toResponse(r);
    }

    @Transactional
    public void delete(RecipientId id) {
        var r = repo.findById(id.value()).orElseThrow(() -> new RecipientNotFoundException(id));
        if (!r.isActive()) {
            return; // idempotent — already soft-deleted, no-op (spec §7.5)
        }
        repo.delete(r);
        events.publishEvent(new RecipientDisabled(r.id(), clock.instant()));
    }

    private @Nullable Matrix resolveMatrix(@Nullable String matrixId) {
        if (matrixId == null) {
            return null;
        }
        return matrixRepo
                .findById(matrixId)
                .orElseThrow(() -> new MatrixNotFoundException(new MatrixId(matrixId)));
    }
}
