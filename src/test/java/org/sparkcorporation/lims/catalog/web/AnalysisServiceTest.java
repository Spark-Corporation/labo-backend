package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisConverted;
import org.sparkcorporation.lims.catalog.api.AnalysisCreated;
import org.sparkcorporation.lims.catalog.api.AnalysisDisabled;
import org.sparkcorporation.lims.catalog.api.AnalysisDuplicated;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisUpdated;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.AnalysisNotFoundException;
import org.sparkcorporation.lims.catalog.domain.ChildMustBeSimpleException;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.DisciplineNotFoundException;
import org.sparkcorporation.lims.catalog.domain.HasComponentsException;
import org.sparkcorporation.lims.catalog.domain.KindAlreadyMatchesException;
import org.sparkcorporation.lims.catalog.domain.MultipleDefaultTechniquesException;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.catalog.domain.UpdateAnalysis;
import org.sparkcorporation.lims.catalog.infra.AnalysisRepository;
import org.sparkcorporation.lims.catalog.infra.DisciplineRepository;
import org.sparkcorporation.lims.catalog.infra.MatrixRepository;
import org.sparkcorporation.lims.catalog.infra.RecipientRepository;
import org.sparkcorporation.lims.catalog.infra.TechniqueRepository;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

class AnalysisServiceTest {

    private final AnalysisRepository repo = mock(AnalysisRepository.class);
    private final DisciplineRepository disciplineRepo = mock(DisciplineRepository.class);
    private final MatrixRepository matrixRepo = mock(MatrixRepository.class);
    private final RecipientRepository recipientRepo = mock(RecipientRepository.class);
    private final TechniqueRepository techniqueRepo = mock(TechniqueRepository.class);
    private final AnalysisMapper mapper = new AnalysisMapper();
    private final AnalysisApi analysisApi = mock(AnalysisApi.class);
    private final ReferenceIntervalService intervalService = mock(ReferenceIntervalService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-07T10:00:00Z"), ZoneOffset.UTC);

    private final AnalysisService service =
            new AnalysisService(
                    repo,
                    disciplineRepo,
                    matrixRepo,
                    recipientRepo,
                    techniqueRepo,
                    mapper,
                    analysisApi,
                    intervalService,
                    events,
                    clock);

    private static Discipline stubDiscipline() {
        return Discipline.create("BIO", "Biochimie", null, DisciplinePrefix.BIOCHIMIE);
    }

    private static NewAnalysis simpleNewCmd(String code) {
        return new NewAnalysis(
                code,
                "Glycémie",
                AnalysisKind.SIMPLE,
                new BigDecimal("10.00"),
                1,
                stubDiscipline().id(),
                null,
                null,
                ResultType.REAL,
                null,
                null,
                null,
                null,
                null);
    }

    private static NewAnalysis groupedNewCmd(String code) {
        return new NewAnalysis(
                code,
                "Bilan glycémique",
                AnalysisKind.GROUPED,
                new BigDecimal("20.00"),
                1,
                stubDiscipline().id(),
                null,
                null,
                ResultType.REAL,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void create_publishes_AnalysisCreated_event() {
        var disc = stubDiscipline();
        when(disciplineRepo.findActiveById(disc.rawId())).thenReturn(Optional.of(disc));
        when(repo.findByCode(any())).thenReturn(Optional.empty());
        when(repo.save(any(Analysis.class))).thenAnswer(inv -> inv.getArgument(0));

        var cmd =
                new NewAnalysis(
                        "GLY",
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
                        null);
        service.create(cmd);
        verify(events).publishEvent(any(AnalysisCreated.class));
    }

    @Test
    void create_rejects_when_code_already_exists() {
        var disc = stubDiscipline();
        when(repo.findByCode("GLY"))
                .thenReturn(
                        Optional.of(Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock)));
        assertThatThrownBy(() -> service.create(simpleNewCmd("GLY")))
                .isInstanceOf(CodeAlreadyExistsException.class);
    }

    @Test
    void create_throws_when_discipline_missing() {
        when(repo.findByCode(any())).thenReturn(Optional.empty());
        when(disciplineRepo.findActiveById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(simpleNewCmd("GLY")))
                .isInstanceOf(DisciplineNotFoundException.class);
    }

    @Test
    void update_publishes_AnalysisUpdated_event() {
        var disc = stubDiscipline();
        var a = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        when(repo.findActiveById(a.rawId())).thenReturn(Optional.of(a));
        var cmd =
                new UpdateAnalysis(
                        "Glycémie 2", null, null, null, null, null, null, null, null, null, null);
        service.update(a.id(), cmd);
        verify(events).publishEvent(any(AnalysisUpdated.class));
        assertThat(a.name()).isEqualTo("Glycémie 2");
    }

    @Test
    void update_throws_when_missing() {
        when(repo.findActiveById(any())).thenReturn(Optional.empty());
        var cmd =
                new UpdateAnalysis("X", null, null, null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> service.update(AnalysisId.generate(), cmd))
                .isInstanceOf(AnalysisNotFoundException.class);
    }

    @Test
    void delete_active_calls_repo_delete_and_publishes_event() {
        var disc = stubDiscipline();
        var a = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        when(repo.findById(a.rawId())).thenReturn(Optional.of(a));
        service.delete(a.id());
        verify(repo).delete(a);
        verify(events).publishEvent(any(AnalysisDisabled.class));
    }

    @Test
    void delete_already_deleted_is_no_op() throws Exception {
        var disc = stubDiscipline();
        var a = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        var deletedAtField = Analysis.class.getDeclaredField("deletedAt");
        deletedAtField.setAccessible(true);
        deletedAtField.set(a, Instant.now());

        when(repo.findById(a.rawId())).thenReturn(Optional.of(a));
        service.delete(a.id());
        verify(repo, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void delete_throws_when_truly_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(AnalysisId.generate()))
                .isInstanceOf(AnalysisNotFoundException.class);
    }

    @Test
    void convertToGrouped_publishes_AnalysisConverted_event() {
        var disc = stubDiscipline();
        var a = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        when(repo.findActiveById(a.rawId())).thenReturn(Optional.of(a));
        service.convertToGrouped(a.id());
        verify(events).publishEvent(any(AnalysisConverted.class));
        assertThat(a.kind()).isEqualTo(AnalysisKind.GROUPED);
    }

    @Test
    void convertToGrouped_rejects_when_already_grouped() {
        var disc = stubDiscipline();
        var a = Analysis.create(groupedNewCmd("BILAN"), disc, null, null, clock);
        when(repo.findActiveById(a.rawId())).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.convertToGrouped(a.id()))
                .isInstanceOf(KindAlreadyMatchesException.class);
    }

    @Test
    void convertToSimple_rejects_when_components_present() {
        var disc = stubDiscipline();
        var parent = Analysis.create(groupedNewCmd("BILAN"), disc, null, null, clock);
        var child = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        parent.addComponent(child, 1);
        when(repo.findActiveById(parent.rawId())).thenReturn(Optional.of(parent));
        assertThatThrownBy(() -> service.convertToSimple(parent.id()))
                .isInstanceOf(HasComponentsException.class);
    }

    @Test
    void convertToSimple_succeeds_when_components_empty() {
        var disc = stubDiscipline();
        var parent = Analysis.create(groupedNewCmd("BILAN"), disc, null, null, clock);
        when(repo.findActiveById(parent.rawId())).thenReturn(Optional.of(parent));
        service.convertToSimple(parent.id());
        assertThat(parent.kind()).isEqualTo(AnalysisKind.SIMPLE);
        verify(events).publishEvent(any(AnalysisConverted.class));
    }

    @Test
    void duplicate_clones_intervals_via_intervalService() {
        var disc = stubDiscipline();
        var source = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        when(repo.findActiveById(source.rawId())).thenReturn(Optional.of(source));
        when(repo.findByCode("GLY_COPY")).thenReturn(Optional.empty());
        when(repo.save(any(Analysis.class))).thenAnswer(inv -> inv.getArgument(0));

        service.duplicate(source.id(), "GLY_COPY");

        verify(intervalService).cloneFromSource(eq(source.id()), any(AnalysisId.class), eq(clock));
        verify(events).publishEvent(any(AnalysisDuplicated.class));
    }

    @Test
    void duplicate_rejects_when_new_code_already_exists() {
        var disc = stubDiscipline();
        var source = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        var other = Analysis.create(simpleNewCmd("GLY_COPY"), disc, null, null, clock);
        when(repo.findActiveById(source.rawId())).thenReturn(Optional.of(source));
        when(repo.findByCode("GLY_COPY")).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.duplicate(source.id(), "GLY_COPY"))
                .isInstanceOf(CodeAlreadyExistsException.class);
    }

    @Test
    void addComponent_throws_when_child_not_simple() {
        var disc = stubDiscipline();
        var parent = Analysis.create(groupedNewCmd("BILAN"), disc, null, null, clock);
        var child = Analysis.create(groupedNewCmd("OTHER_GROUP"), disc, null, null, clock);
        when(repo.findActiveById(parent.rawId())).thenReturn(Optional.of(parent));
        when(repo.findByCode("OTHER_GROUP")).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service.addComponent(parent.id(), "OTHER_GROUP", 1))
                .isInstanceOf(ChildMustBeSimpleException.class);
    }

    @Test
    void addComponent_throws_when_parent_is_simple() {
        var disc = stubDiscipline();
        var parent = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        var child = Analysis.create(simpleNewCmd("CHO"), disc, null, null, clock);
        when(repo.findActiveById(parent.rawId())).thenReturn(Optional.of(parent));
        when(repo.findByCode("CHO")).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service.addComponent(parent.id(), "CHO", 1))
                .isInstanceOf(KindAlreadyMatchesException.class);
    }

    @Test
    void addComponent_persists_link() {
        var disc = stubDiscipline();
        var parent = Analysis.create(groupedNewCmd("BILAN"), disc, null, null, clock);
        var child = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        when(repo.findActiveById(parent.rawId())).thenReturn(Optional.of(parent));
        when(repo.findByCode("GLY")).thenReturn(Optional.of(child));

        service.addComponent(parent.id(), "GLY", 1);
        assertThat(parent.components()).hasSize(1);
        assertThat(parent.components().get(0).childId()).isEqualTo(child.rawId());
    }

    @Test
    void removeComponent_renumbers_remaining_positions() {
        var disc = stubDiscipline();
        var parent = Analysis.create(groupedNewCmd("BILAN"), disc, null, null, clock);
        var c1 = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        var c2 = Analysis.create(simpleNewCmd("CHO"), disc, null, null, clock);
        var c3 = Analysis.create(simpleNewCmd("URE"), disc, null, null, clock);
        parent.addComponent(c1, 1);
        parent.addComponent(c2, 2);
        parent.addComponent(c3, 3);

        when(repo.findActiveById(parent.rawId())).thenReturn(Optional.of(parent));
        when(repo.findByCode("CHO")).thenReturn(Optional.of(c2));

        service.removeComponent(parent.id(), "CHO");

        assertThat(parent.components()).hasSize(2);
        assertThat(parent.components().get(0).childId()).isEqualTo(c1.rawId());
        assertThat(parent.components().get(0).position()).isEqualTo(1);
        assertThat(parent.components().get(1).childId()).isEqualTo(c3.rawId());
        assertThat(parent.components().get(1).position()).isEqualTo(2);
    }

    @Test
    void putTechniques_rejects_multiple_isDefault_true() {
        var disc = stubDiscipline();
        var a = Analysis.create(simpleNewCmd("GLY"), disc, null, null, clock);
        when(repo.findActiveById(a.rawId())).thenReturn(Optional.of(a));

        var items =
                List.of(
                        new PutTechniquesRequest.TechniqueLinkPayload(
                                "01HZB0000000000000000ADMNZ", true),
                        new PutTechniquesRequest.TechniqueLinkPayload(
                                "01HZB0000000000000000XYZAA", true));
        assertThatThrownBy(() -> service.putTechniques(a.id(), items))
                .isInstanceOf(MultipleDefaultTechniquesException.class);
    }

    @Test
    void search_delegates_to_repository() {
        when(repo.search(any(), any(), any())).thenReturn(new ArrayList<>());
        service.search("gly", "BIO", AnalysisKind.SIMPLE);
        verify(repo, times(1)).search("gly", "BIO", AnalysisKind.SIMPLE);
    }

    @Test
    void requireById_uses_disciplineId_in_exception() {
        when(disciplineRepo.findActiveById(any())).thenReturn(Optional.empty());
        when(repo.findByCode(any())).thenReturn(Optional.empty());
        var disciplineId = DisciplineId.next();
        var cmd =
                new NewAnalysis(
                        "GLY",
                        "Glycémie",
                        AnalysisKind.SIMPLE,
                        new BigDecimal("10.00"),
                        1,
                        disciplineId,
                        null,
                        null,
                        ResultType.REAL,
                        null,
                        null,
                        null,
                        null,
                        null);
        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(DisciplineNotFoundException.class);
    }
}
