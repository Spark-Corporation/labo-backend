package org.sparkcorporation.lims.catalog.web;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.IntervalsReplicated;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.AnalysisNotFoundException;
import org.sparkcorporation.lims.catalog.domain.InvalidReplicateRequestException;
import org.sparkcorporation.lims.catalog.domain.NewReferenceInterval;
import org.sparkcorporation.lims.catalog.domain.NoIntervalsToReplicateException;
import org.sparkcorporation.lims.catalog.domain.ReferenceInterval;
import org.sparkcorporation.lims.catalog.infra.AnalysisRepository;
import org.sparkcorporation.lims.catalog.infra.ReferenceIntervalRepository;
import org.sparkcorporation.lims.shared.domain.PatientCategory;

/**
 * Application service for {@link ReferenceInterval}. Owns the replicate workflow (spec §5.7 / §7.3)
 * and the cross-service {@code cloneFromSource} called by {@link AnalysisService#duplicate} after
 * the cloned aggregate is persisted.
 */
@Service
@RequiredArgsConstructor
public class ReferenceIntervalService {

    private final ReferenceIntervalRepository repo;
    private final AnalysisRepository analysisRepo;
    private final ReferenceIntervalMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Clone every active interval from the source analysis onto the clone analysis (used by {@link
     * AnalysisService#duplicate}). Runs in the caller's transaction.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void cloneFromSource(AnalysisId sourceId, AnalysisId cloneId, Clock clock) {
        List<ReferenceInterval> sourceIntervals = repo.findActiveByAnalysisId(sourceId.value());
        for (var s : sourceIntervals) {
            ReferenceInterval clone = s.cloneForAnalysis(cloneId, clock);
            repo.save(clone);
        }
    }

    /** Replicate source-category intervals into target categories per spec §5.7. */
    @Transactional
    public void replicate(
            AnalysisId id,
            PatientCategory source,
            @org.jspecify.annotations.Nullable Set<PatientCategory> targets) {
        Set<PatientCategory> resolvedTargets;
        if (targets == null || targets.isEmpty()) {
            resolvedTargets = EnumSet.complementOf(EnumSet.of(source));
        } else {
            resolvedTargets = EnumSet.copyOf(targets);
        }
        if (resolvedTargets.contains(source)) {
            throw new InvalidReplicateRequestException("source category cannot be in targets");
        }
        Analysis a =
                analysisRepo
                        .findActiveById(id.value())
                        .orElseThrow(() -> new AnalysisNotFoundException(id));
        if (a.kind() != AnalysisKind.SIMPLE) {
            throw new InvalidReplicateRequestException(
                    "intervals only apply to SIMPLE analyses, got " + a.kind());
        }
        List<ReferenceInterval> sourceIntervals =
                repo.findActiveByAnalysisIdAndCategory(id.value(), source);
        if (sourceIntervals.isEmpty()) {
            throw new NoIntervalsToReplicateException(source);
        }
        repo.softDeleteByAnalysisIdAndCategoryIn(id.value(), resolvedTargets);
        int count = 0;
        for (var target : resolvedTargets) {
            for (var s : sourceIntervals) {
                ReferenceInterval clone = s.cloneForCategory(target, clock);
                repo.save(clone);
                count++;
            }
        }
        events.publishEvent(
                new IntervalsReplicated(id, source, resolvedTargets, count, Instant.now(clock)));
    }

    /**
     * Full-replace of the active intervals for an analysis. Existing intervals are soft-deleted and
     * the new payloads (validated by {@link ReferenceInterval#create}) are persisted in their
     * place.
     */
    @Transactional
    public List<ReferenceIntervalResponse> replaceAll(
            AnalysisId id, List<NewReferenceInterval> items) {
        // Verify analysis exists and is active.
        analysisRepo
                .findActiveById(id.value())
                .orElseThrow(() -> new AnalysisNotFoundException(id));
        // Soft-delete existing intervals across all categories.
        var existing = repo.findActiveByAnalysisId(id.value());
        for (var ri : existing) {
            repo.delete(ri);
        }
        return items.stream()
                .map(cmd -> ReferenceInterval.create(cmd, id, clock))
                .map(repo::save)
                .map(mapper::toResponse)
                .toList();
    }
}
