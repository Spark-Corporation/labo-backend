package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.IamUserExistenceCheck;
import org.sparkcorporation.lims.catalog.api.PreleveurCreated;
import org.sparkcorporation.lims.catalog.api.PreleveurDisabled;
import org.sparkcorporation.lims.catalog.api.PreleveurId;
import org.sparkcorporation.lims.catalog.api.PreleveurUpdated;
import org.sparkcorporation.lims.catalog.domain.IamUserNotFoundException;
import org.sparkcorporation.lims.catalog.domain.NewPreleveur;
import org.sparkcorporation.lims.catalog.domain.Preleveur;
import org.sparkcorporation.lims.catalog.domain.PreleveurNotFoundException;
import org.sparkcorporation.lims.catalog.domain.UpdatePreleveur;
import org.sparkcorporation.lims.catalog.infra.CatalogCodeGenerator;
import org.sparkcorporation.lims.catalog.infra.CatalogCodeGenerator.EntityType;
import org.sparkcorporation.lims.catalog.infra.PreleveurRepository;

/**
 * Application service for {@link Preleveur}. Generates the {@code PHL-NNNNNN} code via {@link
 * CatalogCodeGenerator}, preflights {@code userId} via {@link IamUserExistenceCheck} (spec §7) so a
 * non-existent iam user surfaces as {@code IamUserNotFoundException} → 422 instead of a {@code
 * DataIntegrityViolationException} → 500. Implements the spec §7.5 idempotent-delete rule.
 */
@Service
@RequiredArgsConstructor
public class PreleveurService {

    private final PreleveurRepository repo;
    private final CatalogCodeGenerator codeGenerator;
    private final IamUserExistenceCheck iamUserCheck;
    private final PreleveurMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public PreleveurResponse create(NewPreleveur cmd) {
        if (cmd.userId() != null && !iamUserCheck.exists(cmd.userId())) {
            throw new IamUserNotFoundException(cmd.userId());
        }
        String code = codeGenerator.next(EntityType.PRELEVEUR);
        var p = Preleveur.create(cmd, code, clock);
        repo.save(p);
        events.publishEvent(new PreleveurCreated(p.id(), p.code(), Instant.now(clock)));
        return mapper.toResponse(p);
    }

    @Transactional(readOnly = true)
    public PreleveurResponse get(PreleveurId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new PreleveurNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PreleveurSearchResponse list() {
        var items = repo.findAllActive().stream().map(mapper::toResponse).toList();
        return new PreleveurSearchResponse(items);
    }

    @Transactional
    public PreleveurResponse update(PreleveurId id, UpdatePreleveur cmd) {
        var p = repo.findById(id.value()).orElseThrow(() -> new PreleveurNotFoundException(id));
        if (cmd.userId() != null && !iamUserCheck.exists(cmd.userId())) {
            throw new IamUserNotFoundException(cmd.userId());
        }
        p.update(cmd, clock);
        events.publishEvent(new PreleveurUpdated(p.id(), Instant.now(clock)));
        return mapper.toResponse(p);
    }

    @Transactional
    public void delete(PreleveurId id) {
        var p = repo.findById(id.value()).orElseThrow(() -> new PreleveurNotFoundException(id));
        if (!p.isActive()) {
            return; // idempotent — already soft-deleted, no-op (spec §7.5)
        }
        repo.delete(p);
        events.publishEvent(new PreleveurDisabled(p.id(), Instant.now(clock)));
    }
}
