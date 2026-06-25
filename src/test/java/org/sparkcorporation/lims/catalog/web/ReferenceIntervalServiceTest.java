package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.IntervalsReplicated;
import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.AnalysisNotFoundException;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.InvalidReplicateRequestException;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.catalog.domain.NewReferenceInterval;
import org.sparkcorporation.lims.catalog.domain.NoIntervalsToReplicateException;
import org.sparkcorporation.lims.catalog.domain.ReferenceInterval;
import org.sparkcorporation.lims.catalog.infra.AnalysisRepository;
import org.sparkcorporation.lims.catalog.infra.ReferenceIntervalRepository;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.PatientCategory;

class ReferenceIntervalServiceTest {

    private final ReferenceIntervalRepository repo = mock(ReferenceIntervalRepository.class);
    private final AnalysisRepository analysisRepo = mock(AnalysisRepository.class);
    private final ReferenceIntervalMapper mapper = new ReferenceIntervalMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-07T10:00:00Z"), ZoneOffset.UTC);

    private final ReferenceIntervalService service =
            new ReferenceIntervalService(repo, analysisRepo, mapper, events, clock);

    private static Analysis simpleAnalysis(String code) {
        var disc = Discipline.create("BIO", "Biochimie", null, DisciplinePrefix.BIOCHIMIE);
        var cmd =
                new NewAnalysis(
                        code,
                        "Glycémie",
                        AnalysisKind.SIMPLE,
                        new BigDecimal("10.00"),
                        1,
                        disc.id(),
                        null,
                        null,
                        ResultType.REAL,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        return Analysis.create(cmd, disc, null, null, Clock.systemUTC());
    }

    private static Analysis groupedAnalysis(String code) {
        var disc = Discipline.create("BIO", "Biochimie", null, DisciplinePrefix.BIOCHIMIE);
        var cmd =
                new NewAnalysis(
                        code,
                        "Bilan",
                        AnalysisKind.GROUPED,
                        new BigDecimal("20.00"),
                        1,
                        disc.id(),
                        null,
                        null,
                        ResultType.REAL,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        return Analysis.create(cmd, disc, null, null, Clock.systemUTC());
    }

    private static ReferenceInterval standardInterval(AnalysisId id, PatientCategory cat) {
        var cmd =
                new NewReferenceInterval(
                        cat,
                        null,
                        null,
                        null,
                        null,
                        ReferenceMode.STANDARD,
                        new BigDecimal("3.5"),
                        new BigDecimal("5.5"),
                        null,
                        null,
                        null);
        return ReferenceInterval.create(cmd, id, Clock.systemUTC());
    }

    @Test
    void replicate_rejects_when_source_in_targets() {
        var a = simpleAnalysis("GLY");
        when(analysisRepo.findActiveById(a.rawId())).thenReturn(Optional.of(a));
        assertThatThrownBy(
                        () ->
                                service.replicate(
                                        a.id(),
                                        PatientCategory.HOMME,
                                        Set.of(PatientCategory.HOMME, PatientCategory.FEMME)))
                .isInstanceOf(InvalidReplicateRequestException.class);
    }

    @Test
    void replicate_rejects_when_kind_not_simple() {
        var a = groupedAnalysis("BILAN");
        when(analysisRepo.findActiveById(a.rawId())).thenReturn(Optional.of(a));
        assertThatThrownBy(
                        () ->
                                service.replicate(
                                        a.id(),
                                        PatientCategory.HOMME,
                                        EnumSet.of(PatientCategory.FEMME)))
                .isInstanceOf(InvalidReplicateRequestException.class);
    }

    @Test
    void replicate_rejects_when_source_intervals_empty() {
        var a = simpleAnalysis("GLY");
        when(analysisRepo.findActiveById(a.rawId())).thenReturn(Optional.of(a));
        when(repo.findActiveByAnalysisIdAndCategory(a.rawId(), PatientCategory.HOMME))
                .thenReturn(List.of());
        assertThatThrownBy(
                        () ->
                                service.replicate(
                                        a.id(),
                                        PatientCategory.HOMME,
                                        EnumSet.of(PatientCategory.FEMME)))
                .isInstanceOf(NoIntervalsToReplicateException.class);
    }

    @Test
    void replicate_throws_when_analysis_missing() {
        when(analysisRepo.findActiveById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.replicate(
                                        AnalysisId.generate(),
                                        PatientCategory.HOMME,
                                        EnumSet.of(PatientCategory.FEMME)))
                .isInstanceOf(AnalysisNotFoundException.class);
    }

    @Test
    void replicate_replaces_target_intervals_and_publishes_event() {
        var a = simpleAnalysis("GLY");
        when(analysisRepo.findActiveById(a.rawId())).thenReturn(Optional.of(a));
        var sourceInterval = standardInterval(a.id(), PatientCategory.HOMME);
        when(repo.findActiveByAnalysisIdAndCategory(a.rawId(), PatientCategory.HOMME))
                .thenReturn(List.of(sourceInterval));
        when(repo.softDeleteByAnalysisIdAndCategoryIn(eq(a.rawId()), any())).thenReturn(0);
        when(repo.save(any(ReferenceInterval.class))).thenAnswer(inv -> inv.getArgument(0));

        service.replicate(
                a.id(),
                PatientCategory.HOMME,
                EnumSet.of(PatientCategory.FEMME, PatientCategory.ENFANT));

        // Soft-delete invoked once for the target categories.
        verify(repo, times(1))
                .softDeleteByAnalysisIdAndCategoryIn(
                        eq(a.rawId()),
                        eq(EnumSet.of(PatientCategory.FEMME, PatientCategory.ENFANT)));
        // Save invoked once per (target × source intervals) — 2 targets × 1 source = 2 saves.
        verify(repo, times(2)).save(any(ReferenceInterval.class));
        verify(events).publishEvent(any(IntervalsReplicated.class));
    }

    @Test
    void replicate_with_empty_targets_defaults_to_complement() {
        var a = simpleAnalysis("GLY");
        when(analysisRepo.findActiveById(a.rawId())).thenReturn(Optional.of(a));
        var sourceInterval = standardInterval(a.id(), PatientCategory.HOMME);
        when(repo.findActiveByAnalysisIdAndCategory(a.rawId(), PatientCategory.HOMME))
                .thenReturn(List.of(sourceInterval));
        when(repo.save(any(ReferenceInterval.class))).thenAnswer(inv -> inv.getArgument(0));

        service.replicate(a.id(), PatientCategory.HOMME, null);

        // 3 other categories (FEMME, ENFANT, NOURRISSON_NOUVEAU_NE) × 1 source = 3 saves.
        verify(repo, times(3)).save(any(ReferenceInterval.class));
    }

    @Test
    void cloneFromSource_clones_each_active_interval() {
        var sourceId = AnalysisId.generate();
        var cloneId = AnalysisId.generate();
        var i1 = standardInterval(sourceId, PatientCategory.HOMME);
        var i2 = standardInterval(sourceId, PatientCategory.FEMME);
        when(repo.findActiveByAnalysisId(sourceId.value())).thenReturn(List.of(i1, i2));
        when(repo.save(any(ReferenceInterval.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cloneFromSource(sourceId, cloneId, clock);

        verify(repo, times(2)).save(any(ReferenceInterval.class));
    }

    @Test
    void cloneFromSource_with_no_intervals_is_noop() {
        var sourceId = AnalysisId.generate();
        var cloneId = AnalysisId.generate();
        when(repo.findActiveByAnalysisId(sourceId.value())).thenReturn(List.of());
        service.cloneFromSource(sourceId, cloneId, clock);
        verify(repo, never()).save(any());
    }

    @Test
    void replaceAll_throws_when_analysis_missing() {
        when(analysisRepo.findActiveById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.replaceAll(AnalysisId.generate(), List.of()))
                .isInstanceOf(AnalysisNotFoundException.class);
    }

    @Test
    void replaceAll_soft_deletes_existing_then_persists_new() {
        var a = simpleAnalysis("GLY");
        when(analysisRepo.findActiveById(a.rawId())).thenReturn(Optional.of(a));
        var existing = standardInterval(a.id(), PatientCategory.HOMME);
        when(repo.findActiveByAnalysisId(a.rawId())).thenReturn(List.of(existing));
        when(repo.save(any(ReferenceInterval.class))).thenAnswer(inv -> inv.getArgument(0));

        var newCmd =
                new NewReferenceInterval(
                        PatientCategory.FEMME,
                        null,
                        null,
                        null,
                        null,
                        ReferenceMode.STANDARD,
                        new BigDecimal("3.0"),
                        new BigDecimal("5.0"),
                        null,
                        null,
                        null);

        service.replaceAll(a.id(), List.of(newCmd));

        verify(repo).delete(existing);
        verify(repo, times(1)).save(any(ReferenceInterval.class));
    }
}
