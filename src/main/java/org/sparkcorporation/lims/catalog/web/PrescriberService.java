package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.PrescriberCreated;
import org.sparkcorporation.lims.catalog.api.PrescriberDisabled;
import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.catalog.api.PrescriberUpdated;
import org.sparkcorporation.lims.catalog.api.SpecialtyId;
import org.sparkcorporation.lims.catalog.domain.NewPrescriber;
import org.sparkcorporation.lims.catalog.domain.Prescriber;
import org.sparkcorporation.lims.catalog.domain.PrescriberNotFoundException;
import org.sparkcorporation.lims.catalog.domain.Specialty;
import org.sparkcorporation.lims.catalog.domain.SpecialtyNotFoundException;
import org.sparkcorporation.lims.catalog.domain.UpdatePrescriber;
import org.sparkcorporation.lims.catalog.infra.CatalogCodeGenerator;
import org.sparkcorporation.lims.catalog.infra.CatalogCodeGenerator.EntityType;
import org.sparkcorporation.lims.catalog.infra.PrescriberRepository;
import org.sparkcorporation.lims.catalog.infra.SpecialtyRepository;

/**
 * Application service for {@link Prescriber}. Resolves {@code specialtyId} to an active {@link
 * Specialty} aggregate before invoking the domain factory; throws {@link
 * SpecialtyNotFoundException} when the FK does not resolve to an active specialty (spec §7).
 *
 * <p>{@code code} is server-generated as {@code PRS-NNNNNN} via {@link CatalogCodeGenerator}.
 * Implements the spec §7.5 idempotent-delete rule.
 */
@Service
@RequiredArgsConstructor
public class PrescriberService {

    private final PrescriberRepository repo;
    private final SpecialtyRepository specialtyRepo;
    private final CatalogCodeGenerator codeGenerator;
    private final PrescriberMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public PrescriberResponse create(NewPrescriber cmd) {
        Specialty specialty = resolveSpecialty(cmd.specialtyId());
        String code = codeGenerator.next(EntityType.PRESCRIBER);
        var p = Prescriber.create(cmd, code, specialty, clock);
        repo.save(p);
        events.publishEvent(new PrescriberCreated(p.id(), p.code(), Instant.now(clock)));
        return mapper.toResponse(p);
    }

    @Transactional(readOnly = true)
    public PrescriberResponse get(PrescriberId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new PrescriberNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PrescriberSearchResponse search(@Nullable String q, @Nullable String specialtyCode) {
        var items = repo.search(q, specialtyCode).stream().map(mapper::toResponse).toList();
        return new PrescriberSearchResponse(items);
    }

    @Transactional
    public PrescriberResponse update(PrescriberId id, UpdatePrescriber cmd) {
        var p = repo.findById(id.value()).orElseThrow(() -> new PrescriberNotFoundException(id));
        Specialty newSpecialty =
                cmd.specialtyId() == null ? null : resolveSpecialty(cmd.specialtyId());
        p.update(cmd, newSpecialty, clock);
        events.publishEvent(new PrescriberUpdated(p.id(), Instant.now(clock)));
        return mapper.toResponse(p);
    }

    @Transactional
    public void delete(PrescriberId id) {
        var p = repo.findById(id.value()).orElseThrow(() -> new PrescriberNotFoundException(id));
        if (!p.isActive()) {
            return; // idempotent — already soft-deleted, no-op (spec §7.5)
        }
        repo.delete(p);
        events.publishEvent(new PrescriberDisabled(p.id(), Instant.now(clock)));
    }

    private @Nullable Specialty resolveSpecialty(@Nullable SpecialtyId specialtyId) {
        if (specialtyId == null) {
            return null;
        }
        return specialtyRepo
                .findActiveById(specialtyId.value())
                .orElseThrow(() -> new SpecialtyNotFoundException(specialtyId));
    }
}
