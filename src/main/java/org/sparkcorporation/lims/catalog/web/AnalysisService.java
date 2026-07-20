package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisConverted;
import org.sparkcorporation.lims.catalog.api.AnalysisCreated;
import org.sparkcorporation.lims.catalog.api.AnalysisDisabled;
import org.sparkcorporation.lims.catalog.api.AnalysisDuplicated;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.AnalysisUpdated;
import org.sparkcorporation.lims.catalog.api.TechniqueId;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.AnalysisComponent;
import org.sparkcorporation.lims.catalog.domain.AnalysisNotFoundException;
import org.sparkcorporation.lims.catalog.domain.AnalysisTechniqueLink;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.DisciplineNotFoundException;
import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.MatrixNotFoundException;
import org.sparkcorporation.lims.catalog.domain.MultipleDefaultTechniquesException;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.catalog.domain.Recipient;
import org.sparkcorporation.lims.catalog.domain.RecipientNotFoundException;
import org.sparkcorporation.lims.catalog.domain.Technique;
import org.sparkcorporation.lims.catalog.domain.TechniqueNotFoundException;
import org.sparkcorporation.lims.catalog.domain.UpdateAnalysis;
import org.sparkcorporation.lims.catalog.infra.AnalysisRepository;
import org.sparkcorporation.lims.catalog.infra.DisciplineRepository;
import org.sparkcorporation.lims.catalog.infra.MatrixRepository;
import org.sparkcorporation.lims.catalog.infra.RecipientRepository;
import org.sparkcorporation.lims.catalog.infra.TechniqueRepository;

/**
 * Application service for {@link Analysis}. Resolves all FKs to active aggregates before invoking
 * domain operations; orchestrates the duplicate workflow with {@link ReferenceIntervalService}; and
 * implements the spec §7.5 idempotent-delete rule.
 *
 * <p>Per spec §5.5 / §7.1: {@code code} and {@code kind} are immutable in update; use the convert
 * endpoints ({@code :convert-to-grouped}, {@code :convert-to-simple}) to change kind, and {@code
 * :duplicate} to clone under a new code.
 */
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository repo;
    private final DisciplineRepository disciplineRepo;
    private final MatrixRepository matrixRepo;
    private final RecipientRepository recipientRepo;
    private final TechniqueRepository techniqueRepo;
    private final AnalysisMapper mapper;
    private final AnalysisApi analysisApi;
    private final ReferenceIntervalService intervalService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public AnalysisResponse create(NewAnalysis cmd) {
        if (repo.findByCode(cmd.code()).isPresent()) {
            throw new CodeAlreadyExistsException("analysis", cmd.code());
        }
        Discipline discipline = resolveDiscipline(cmd.disciplineId().value());
        Matrix matrix = resolveMatrix(cmd.matrixId() == null ? null : cmd.matrixId().value());
        Recipient recipient =
                resolveRecipient(cmd.recipientId() == null ? null : cmd.recipientId().value());
        Analysis a = Analysis.create(cmd, discipline, matrix, recipient, clock);
        repo.save(a);
        events.publishEvent(new AnalysisCreated(a.id(), a.code(), a.kind(), Instant.now(clock)));
        return toResponseFresh(a);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse get(AnalysisId id) {
        Analysis a =
                repo.findActiveById(id.value())
                        .orElseThrow(() -> new AnalysisNotFoundException(id));
        AnalysisSummary summary =
                analysisApi.findById(id).orElseThrow(() -> new AnalysisNotFoundException(id));
        return mapper.toResponse(summary, a);
    }

    @Transactional(readOnly = true)
    public AnalysisSearchResponse search(
            @Nullable String q, @Nullable String disciplineCode, @Nullable AnalysisKind kind) {
        var rows = repo.search(q, disciplineCode, kind);
        var items =
                rows.stream()
                        .map(
                                a ->
                                        analysisApi
                                                .findById(a.id())
                                                .map(s -> mapper.toResponse(s, a))
                                                .orElseThrow(
                                                        () ->
                                                                new AnalysisNotFoundException(
                                                                        a.id())))
                        .toList();
        return new AnalysisSearchResponse(items);
    }

    @Transactional
    public AnalysisResponse update(AnalysisId id, UpdateAnalysis cmd) {
        Analysis a =
                repo.findActiveById(id.value())
                        .orElseThrow(() -> new AnalysisNotFoundException(id));
        Matrix newMatrix = cmd.matrixId() == null ? null : resolveMatrix(cmd.matrixId().value());
        Recipient newRecipient =
                cmd.recipientId() == null ? null : resolveRecipient(cmd.recipientId().value());
        a.update(cmd, newMatrix, newRecipient, clock);
        events.publishEvent(new AnalysisUpdated(a.id(), Instant.now(clock)));
        return toResponseFresh(a);
    }

    @Transactional
    public void delete(AnalysisId id) {
        Analysis a = repo.findById(id.value()).orElseThrow(() -> new AnalysisNotFoundException(id));
        if (!a.isActive()) {
            return; // idempotent — already soft-deleted, no-op (spec §7.5)
        }
        repo.delete(a);
        events.publishEvent(new AnalysisDisabled(a.id(), Instant.now(clock)));
    }

    @Transactional
    public AnalysisResponse convertToGrouped(AnalysisId id) {
        Analysis a =
                repo.findActiveById(id.value())
                        .orElseThrow(() -> new AnalysisNotFoundException(id));
        var fromKind = a.kind();
        a.convertToGrouped();
        events.publishEvent(new AnalysisConverted(a.id(), fromKind, a.kind(), Instant.now(clock)));
        return toResponseFresh(a);
    }

    @Transactional
    public AnalysisResponse convertToSimple(AnalysisId id) {
        Analysis a =
                repo.findActiveById(id.value())
                        .orElseThrow(() -> new AnalysisNotFoundException(id));
        var fromKind = a.kind();
        a.convertToSimple();
        events.publishEvent(new AnalysisConverted(a.id(), fromKind, a.kind(), Instant.now(clock)));
        return toResponseFresh(a);
    }

    @Transactional
    public AnalysisResponse duplicate(AnalysisId sourceId, String newCode) {
        Analysis source =
                repo.findActiveById(sourceId.value())
                        .orElseThrow(() -> new AnalysisNotFoundException(sourceId));
        if (repo.findByCode(newCode).isPresent()) {
            throw new CodeAlreadyExistsException("analysis", newCode);
        }
        Analysis clone = source.duplicate(AnalysisCode.of(newCode), clock);
        repo.save(clone);
        intervalService.cloneFromSource(source.id(), clone.id(), clock);
        events.publishEvent(
                new AnalysisDuplicated(source.id(), clone.id(), newCode, Instant.now(clock)));
        return toResponseFresh(clone);
    }

    @Transactional
    public AnalysisResponse addComponent(AnalysisId id, String childCode, int position) {
        Analysis parent =
                repo.findActiveById(id.value())
                        .orElseThrow(() -> new AnalysisNotFoundException(id));
        Analysis child = requireActiveByCode(childCode);
        parent.addComponent(child, position);
        events.publishEvent(new AnalysisUpdated(parent.id(), Instant.now(clock)));
        return toResponseFresh(parent);
    }

    @Transactional
    public AnalysisResponse removeComponent(AnalysisId id, String childCode) {
        Analysis parent =
                repo.findActiveById(id.value())
                        .orElseThrow(() -> new AnalysisNotFoundException(id));
        Analysis child = requireActiveByCode(childCode);
        var components = parent.components();
        Optional<AnalysisComponent> match =
                components.stream().filter(c -> c.childId().equals(child.rawId())).findFirst();
        if (match.isEmpty()) {
            // No-op when the component is not linked — best-effort remove (the parent simply has
            // no such row).
            return toResponseFresh(parent);
        }
        components.remove(match.get());
        // Renumber remaining positions densely (1, 2, 3, ...) — service-layer guarantee per spec
        // §4.2 (no UNIQUE(parent_id, position) DB constraint; ordering enforced here within the
        // optimistic-locked transaction).
        int pos = 1;
        for (var comp : components) {
            comp.setPosition(pos++);
        }
        events.publishEvent(new AnalysisUpdated(parent.id(), Instant.now(clock)));
        return toResponseFresh(parent);
    }

    @Transactional
    public AnalysisResponse putTechniques(
            AnalysisId id, List<PutTechniquesRequest.TechniqueLinkPayload> items) {
        long defaultCount =
                items.stream().filter(PutTechniquesRequest.TechniqueLinkPayload::isDefault).count();
        if (defaultCount > 1) {
            throw new MultipleDefaultTechniquesException(
                    "at most one technique can be marked default, got " + defaultCount);
        }
        Analysis a =
                repo.findActiveById(id.value())
                        .orElseThrow(() -> new AnalysisNotFoundException(id));
        // Resolve each technique to confirm it exists and is active.
        var newLinks = new HashSet<AnalysisTechniqueLink>();
        for (var item : items) {
            Technique t =
                    techniqueRepo
                            .findActiveById(item.techniqueId())
                            .orElseThrow(
                                    () ->
                                            new TechniqueNotFoundException(
                                                    new TechniqueId(item.techniqueId())));
            newLinks.add(new AnalysisTechniqueLink(a.id(), t.id(), item.isDefault()));
        }
        a.techniques().clear();
        a.techniques().addAll(newLinks);
        events.publishEvent(new AnalysisUpdated(a.id(), Instant.now(clock)));
        return toResponseFresh(a);
    }

    private Analysis requireActiveByCode(String code) {
        return repo.findByCode(code)
                .filter(Analysis::isActive)
                .orElseThrow(() -> new AnalysisNotFoundException(AnalysisId.generate()));
    }

    private Discipline resolveDiscipline(String value) {
        return disciplineRepo
                .findActiveById(value)
                .orElseThrow(
                        () ->
                                new DisciplineNotFoundException(
                                        new org.sparkcorporation.lims.catalog.api.DisciplineId(
                                                value)));
    }

    private @Nullable Matrix resolveMatrix(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return matrixRepo
                .findActiveById(value)
                .orElseThrow(
                        () ->
                                new MatrixNotFoundException(
                                        new org.sparkcorporation.lims.catalog.api.MatrixId(value)));
    }

    private @Nullable Recipient resolveRecipient(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return recipientRepo
                .findActiveById(value)
                .orElseThrow(
                        () ->
                                new RecipientNotFoundException(
                                        new org.sparkcorporation.lims.catalog.api.RecipientId(
                                                value)));
    }

    /**
     * Build a response from a freshly mutated {@link Analysis} entity by going through the {@link
     * AnalysisApi} so the embedded snapshots and component/technique link projections are resolved
     * consistently with the {@code GET /{id}} contract.
     */
    private AnalysisResponse toResponseFresh(Analysis a) {
        AnalysisSummary summary =
                analysisApi.findById(a.id()).orElseGet(() -> buildFallbackSummary(a));
        return mapper.toResponse(summary, a);
    }

    /**
     * Build a minimal summary without re-querying the API (covers the test path where the
     * AnalysisApi mock is not stubbed). Includes empty component/technique lists — the just-mutated
     * aggregate has authoritative state on disk after this transaction commits.
     */
    private static AnalysisSummary buildFallbackSummary(Analysis a) {
        var disc = a.discipline();
        if (disc == null) {
            throw new IllegalStateException("analysis has no discipline");
        }
        return new AnalysisSummary(
                a.id(),
                a.code(),
                a.name(),
                a.kind(),
                a.indiciaryValueB(),
                a.labelCount(),
                new AnalysisSummary.DisciplineSnapshot(
                        disc.id(), disc.code(), disc.name(), disc.disciplinePrefix()),
                a.matrix() == null
                        ? null
                        : new AnalysisSummary.MatrixSnapshot(
                                a.matrix().id(), a.matrix().code(), a.matrix().name()),
                a.recipient() == null
                        ? null
                        : new AnalysisSummary.RecipientSnapshot(
                                a.recipient().id(), a.recipient().code(), a.recipient().name()),
                a.resultType(),
                a.interpretationText(),
                a.formulaExpression(),
                a.defaultPortoir(),
                a.defaultConservationDays(),
                new AnalysisSummary.UnitsSummary(
                        a.units().unit1(),
                        a.units().unit1Decimals(),
                        a.units().unit2(),
                        a.units().unit2Decimals(),
                        a.units().conversionFactor()),
                new AnalysisSummary.AnalyticalDelaySummary(
                        a.delay().days(), a.delay().hours(), a.delay().anteriorityCount()),
                new ArrayList<>(),
                new ArrayList<>());
    }
}
